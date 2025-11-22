package com.reliaquest.api.domain;

import com.reliaquest.api.adapter.ApiResponse;
import com.reliaquest.api.config.EmployeeClientProperties;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.TooManyRequestsException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    // Not final due to CGLIB proxy requirement - Spring manages lifecycle
    private WebClient webClient;

    @Autowired
    public EmployeeService(WebClient.Builder webClientBuilder, EmployeeClientProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.readTimeout())
                .option(
                        io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.connectTimeout().toMillis());

        this.webClient = webClientBuilder
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    // Required for CGLIB proxy - Objenesis bypasses this constructor at runtime
    protected EmployeeService() {}

    // Protected constructor for testing with custom WebClient
    protected EmployeeService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Cacheable(value = "employees", key = "'all'")
    @Retryable(
            retryFor = TooManyRequestsException.class,
            noRetryFor = EmployeeNotFoundException.class,
            maxAttemptsExpression = "#{${employee.client.retry.max-attempts:3}}",
            backoff =
                    @Backoff(
                            delayExpression = "#{${employee.client.retry.delay:500}}",
                            multiplierExpression = "#{${employee.client.retry.multiplier:2.0}}"))
    public List<Employee> getAllEmployees() {
        String correlationId = getCorrelationId();
        logger.debug("Cache MISS - Fetching all employees correlationId={}", correlationId);

        ApiResponse<List<Employee>> response = webClient
                .get()
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r -> handle4xxError(r, "fetching all employees"))
                .onStatus(HttpStatusCode::is5xxServerError, r -> handle5xxError(r, "fetching all employees"))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {})
                .block();

        List<Employee> employees = response != null && response.data() != null ? response.data() : List.of();
        return Collections.unmodifiableList(employees);
    }

    public List<Employee> searchByName(String searchString) {
        logger.debug("Searching employees by name={} correlationId={}", searchString, getCorrelationId());
        if (searchString == null || searchString.isBlank()) {
            return getAllEmployees();
        }
        String lowerSearchString = searchString.toLowerCase();
        return getAllEmployees().stream()
                .filter(e -> e.name() != null && e.name().toLowerCase().contains(lowerSearchString))
                .toList();
    }

    public Employee getById(UUID id) {
        String correlationId = getCorrelationId();
        logger.debug("Finding employee by id={} correlationId={}", id, correlationId);

        // Mock server doesn't provide a GET by ID endpoint, so we fetch all and filter
        // This is a known limitation that would cause performance issues at scale
        Optional<Employee> employee =
                getAllEmployees().stream().filter(e -> e.id().equals(id)).findFirst();

        return employee.orElseThrow(() -> {
            logger.warn("Employee not found id={} correlationId={}", id, correlationId);
            return new EmployeeNotFoundException(id);
        });
    }

    public int getHighestSalary() {
        logger.debug("Getting highest salary correlationId={}", getCorrelationId());
        return getAllEmployees().stream()
                .filter(e -> e.salary() != null)
                .mapToInt(Employee::salary)
                .max()
                .orElse(0);
    }

    public List<String> getTopTenHighestEarningNames() {
        logger.debug("Getting top ten highest earning names correlationId={}", getCorrelationId());
        return getAllEmployees().stream()
                .filter(e -> e.salary() != null)
                .sorted(Comparator.comparingInt(Employee::salary).reversed())
                .limit(10)
                .map(Employee::name)
                .toList();
    }

    @CacheEvict(value = "employees", key = "'all'")
    @Retryable(
            retryFor = TooManyRequestsException.class,
            noRetryFor = EmployeeNotFoundException.class,
            maxAttemptsExpression = "#{${employee.client.retry.max-attempts:3}}",
            backoff =
                    @Backoff(
                            delayExpression = "#{${employee.client.retry.delay:500}}",
                            multiplierExpression = "#{${employee.client.retry.multiplier:2.0}}"))
    public Employee create(CreateEmployeeRequest request) {
        String correlationId = getCorrelationId();
        logger.info("Creating employee name={} correlationId={}", request.name(), correlationId);

        ApiResponse<Employee> response = webClient
                .post()
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r -> handle4xxError(r, "creating employee"))
                .onStatus(HttpStatusCode::is5xxServerError, r -> handle5xxError(r, "creating employee"))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Employee>>() {})
                .block();

        if (response == null || response.data() == null) {
            throw new EmployeeServiceException("Failed to create employee - no data returned", HttpStatus.BAD_GATEWAY);
        }

        return response.data();
    }

    /**
     * Delete an employee by ID.
     *
     * <p>Note: Due to the mock server API requiring deletion by name, there is a potential race
     * condition between finding the employee and deleting by name. Another process could delete the
     * employee or modify its name between these operations.
     *
     * @param id the employee ID
     * @return the deleted employee's name
     * @throws EmployeeNotFoundException if employee not found
     * @throws EmployeeServiceException if deletion fails
     */
    @CacheEvict(value = "employees", key = "'all'")
    @Retryable(
            retryFor = TooManyRequestsException.class,
            noRetryFor = EmployeeNotFoundException.class,
            maxAttemptsExpression = "#{${employee.client.retry.max-attempts:3}}",
            backoff =
                    @Backoff(
                            delayExpression = "#{${employee.client.retry.delay:500}}",
                            multiplierExpression = "#{${employee.client.retry.multiplier:2.0}}"))
    public String deleteById(UUID id) {
        Employee employee = getById(id);
        String correlationId = getCorrelationId();
        logger.info("Deleting employee id={} name={} correlationId={}", id, employee.name(), correlationId);

        // Mock server expects {"name": "..."} in request body for DELETE
        Map<String, String> requestBody = Map.of("name", employee.name());

        ApiResponse<Boolean> response = webClient
                .method(HttpMethod.DELETE)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r -> handle4xxError(r, "deleting employee"))
                .onStatus(HttpStatusCode::is5xxServerError, r -> handle5xxError(r, "deleting employee"))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {})
                .block();

        boolean deleted = response != null && Boolean.TRUE.equals(response.data());
        if (!deleted) {
            logger.error(
                    "Failed to delete employee id={} name={} correlationId={}", id, employee.name(), correlationId);
            throw new EmployeeServiceException(
                    "Failed to delete employee id=" + id + " name=" + employee.name(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return employee.name();
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : "unknown";
    }

    private Mono<? extends Throwable> handle4xxError(ClientResponse response, String operation) {
        int statusCode = response.statusCode().value();
        if (statusCode == 429) {
            logger.warn("Rate limited while {} correlationId={}, will retry", operation, getCorrelationId());
            return response
                    .bodyToMono(String.class)
                    .defaultIfEmpty("Too many requests")
                    .map(body -> new TooManyRequestsException("Rate limited: " + body));
        }
        return response
                .bodyToMono(String.class)
                .defaultIfEmpty("Unknown client error")
                .map(body -> new EmployeeServiceException(
                        "Client error while " + operation + ": " + body, HttpStatus.BAD_GATEWAY));
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse response, String operation) {
        int statusCode = response.statusCode().value();
        logger.error("Server error {} while {} correlationId={}", statusCode, operation, getCorrelationId());
        return response
                .bodyToMono(String.class)
                .defaultIfEmpty("Unknown server error")
                .map(body -> new EmployeeServiceException(
                        "Server error while " + operation + ": " + body, HttpStatus.BAD_GATEWAY));
    }

    @Recover
    public List<Employee> recoverGetAllEmployees(TooManyRequestsException e) {
        logger.error("All retry attempts exhausted for getAllEmployees correlationId={}", getCorrelationId(), e);
        throw new EmployeeServiceException(
                "Employee service temporarily unavailable after retries", HttpStatus.SERVICE_UNAVAILABLE, e);
    }

    @Recover
    public Employee recoverCreate(TooManyRequestsException e, CreateEmployeeRequest request) {
        logger.error(
                "All retry attempts exhausted for create name={} correlationId={}",
                request.name(),
                getCorrelationId(),
                e);
        throw new EmployeeServiceException(
                "Employee service temporarily unavailable after retries", HttpStatus.SERVICE_UNAVAILABLE, e);
    }

    @Recover
    public String recoverDeleteById(TooManyRequestsException e, UUID id) {
        logger.error(
                "All retry attempts exhausted for delete id={} correlationId={}", id, getCorrelationId(), e);
        throw new EmployeeServiceException(
                "Employee service temporarily unavailable after retries", HttpStatus.SERVICE_UNAVAILABLE, e);
    }

    // Spring Retry requires a recovery method for exceptions thrown within @Retryable methods.
    // This re-throws EmployeeNotFoundException so GlobalExceptionHandler can return 404.
    @Recover
    public String recoverDeleteByIdNotFound(EmployeeNotFoundException e, UUID id) {
        throw e;
    }
}
