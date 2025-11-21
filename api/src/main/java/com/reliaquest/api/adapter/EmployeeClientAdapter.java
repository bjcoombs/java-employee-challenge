package com.reliaquest.api.adapter;

import com.reliaquest.api.config.EmployeeClientProperties;
import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.domain.port.EmployeePort;
import com.reliaquest.api.exception.ExternalServiceException;
import com.reliaquest.api.exception.ServiceUnavailableException;
import com.reliaquest.api.exception.TooManyRequestsException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Component
public class EmployeeClientAdapter implements EmployeePort {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeClientAdapter.class);

    private final WebClient webClient;

    @Autowired
    public EmployeeClientAdapter(WebClient.Builder webClientBuilder, EmployeeClientProperties properties) {
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

    // Required for CGLIB proxy; Objenesis bypasses this constructor at runtime
    protected EmployeeClientAdapter() {
        this.webClient = null;
    }

    // Protected constructor for testing with custom base URL
    protected EmployeeClientAdapter(WebClient.Builder webClientBuilder, String baseUrl, EmployeeClientProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.readTimeout())
                .option(
                        io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.connectTimeout().toMillis());

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    @Retryable(
            retryFor = TooManyRequestsException.class,
            maxAttemptsExpression = "#{${employee.client.retry.max-attempts:3}}",
            backoff = @Backoff(delayExpression = "#{${employee.client.retry.delay:500}}", multiplierExpression = "#{${employee.client.retry.multiplier:2.0}}"))
    public List<Employee> findAll() {
        String correlationId = getCorrelationId();
        logger.info("Fetching all employees correlationId={}", correlationId);

        ApiResponse<List<Employee>> response = webClient
                .get()
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r -> handle4xxError(r, "fetching all employees"))
                .onStatus(HttpStatusCode::is5xxServerError, r -> handle5xxError(r, "fetching all employees"))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {})
                .block();

        return response != null && response.data() != null ? response.data() : List.of();
    }

    @Override
    public Optional<Employee> findById(UUID id) {
        String correlationId = getCorrelationId();
        logger.info("Fetching employee by id={} correlationId={}", id, correlationId);

        // Mock server doesn't provide a GET by ID endpoint, so we fetch all and filter
        // This is a known limitation that would cause performance issues at scale
        // Note: @Retryable not needed here as findAll() already handles retry
        List<Employee> allEmployees = findAll();
        return allEmployees.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    @Override
    @Retryable(
            retryFor = TooManyRequestsException.class,
            maxAttemptsExpression = "#{${employee.client.retry.max-attempts:3}}",
            backoff = @Backoff(delayExpression = "#{${employee.client.retry.delay:500}}", multiplierExpression = "#{${employee.client.retry.multiplier:2.0}}"))
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
            throw new ExternalServiceException("Failed to create employee - no data returned", 0);
        }

        return response.data();
    }

    @Override
    @Retryable(
            retryFor = TooManyRequestsException.class,
            maxAttemptsExpression = "#{${employee.client.retry.max-attempts:3}}",
            backoff = @Backoff(delayExpression = "#{${employee.client.retry.delay:500}}", multiplierExpression = "#{${employee.client.retry.multiplier:2.0}}"))
    public boolean deleteByName(String name) {
        String correlationId = getCorrelationId();
        logger.info("Deleting employee name={} correlationId={}", name, correlationId);

        // Mock server expects {"name": "..."} in request body for DELETE
        Map<String, String> requestBody = Map.of("name", name);

        ApiResponse<Boolean> response = webClient
                .method(org.springframework.http.HttpMethod.DELETE)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r -> handle4xxError(r, "deleting employee"))
                .onStatus(HttpStatusCode::is5xxServerError, r -> handle5xxError(r, "deleting employee"))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {})
                .block();

        return response != null && Boolean.TRUE.equals(response.data());
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
                .map(body -> new ExternalServiceException("Client error while " + operation + ": " + body, statusCode));
    }

    private Mono<? extends Throwable> handle5xxError(ClientResponse response, String operation) {
        int statusCode = response.statusCode().value();
        logger.error("Server error {} while {} correlationId={}", statusCode, operation, getCorrelationId());
        return response
                .bodyToMono(String.class)
                .defaultIfEmpty("Unknown server error")
                .map(body -> new ExternalServiceException("Server error while " + operation + ": " + body, statusCode));
    }

    @Recover
    public List<Employee> recoverFindAll(TooManyRequestsException e) {
        logger.error("All retry attempts exhausted for findAll correlationId={}", getCorrelationId(), e);
        throw new ServiceUnavailableException("Employee service temporarily unavailable after retries", e);
    }

    @Recover
    public Employee recoverCreate(TooManyRequestsException e, CreateEmployeeRequest request) {
        logger.error(
                "All retry attempts exhausted for create name={} correlationId={}",
                request.name(),
                getCorrelationId(),
                e);
        throw new ServiceUnavailableException("Employee service temporarily unavailable after retries", e);
    }

    @Recover
    public boolean recoverDeleteByName(TooManyRequestsException e, String name) {
        logger.error(
                "All retry attempts exhausted for delete name={} correlationId={}", name, getCorrelationId(), e);
        throw new ServiceUnavailableException("Employee service temporarily unavailable after retries", e);
    }
}
