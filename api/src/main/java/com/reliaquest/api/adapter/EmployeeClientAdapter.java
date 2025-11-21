package com.reliaquest.api.adapter;

import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.domain.port.EmployeePort;
import com.reliaquest.api.exception.TooManyRequestsException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
public class EmployeeClientAdapter implements EmployeePort {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeClientAdapter.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:8112/api/v1/employee";

    private final WebClient webClient;

    public EmployeeClientAdapter(WebClient.Builder webClientBuilder) {
        this(webClientBuilder, DEFAULT_BASE_URL);
    }

    // Package-private constructor for CGLIB proxy required by @Retryable
    EmployeeClientAdapter() {
        this.webClient = null;
    }

    protected EmployeeClientAdapter(WebClient.Builder webClientBuilder, String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    @Retryable(
            retryFor = TooManyRequestsException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public List<Employee> findAll() {
        logger.info("Fetching all employees");

        ApiResponse<List<Employee>> response = webClient
                .get()
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        clientResponse -> {
                            if (clientResponse.statusCode().value() == 429) {
                                logger.warn("Rate limited while fetching all employees, will retry");
                                return clientResponse
                                        .bodyToMono(String.class)
                                        .map(body -> new TooManyRequestsException("Rate limited: " + body));
                            }
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Client error: " + body));
                        })
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        clientResponse -> clientResponse
                                .bodyToMono(String.class)
                                .map(body -> new RuntimeException("Server error: " + body)))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {})
                .block();

        return response != null && response.data() != null ? response.data() : List.of();
    }

    @Override
    @Retryable(
            retryFor = TooManyRequestsException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public Optional<Employee> findById(UUID id) {
        logger.info("Fetching employee by id: {}", id);

        // Mock server doesn't provide a GET by ID endpoint, so we fetch all and filter
        // This is a known limitation that would cause performance issues at scale
        List<Employee> allEmployees = findAll();
        return allEmployees.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    @Override
    @Retryable(
            retryFor = TooManyRequestsException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public Employee create(CreateEmployeeRequest request) {
        logger.info("Creating employee: {}", request.name());

        ApiResponse<Employee> response = webClient
                .post()
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        clientResponse -> {
                            if (clientResponse.statusCode().value() == 429) {
                                logger.warn("Rate limited while creating employee, will retry");
                                return clientResponse
                                        .bodyToMono(String.class)
                                        .map(body -> new TooManyRequestsException("Rate limited: " + body));
                            }
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Client error: " + body));
                        })
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        clientResponse -> clientResponse
                                .bodyToMono(String.class)
                                .map(body -> new RuntimeException("Server error: " + body)))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Employee>>() {})
                .block();

        if (response == null || response.data() == null) {
            throw new RuntimeException("Failed to create employee");
        }

        return response.data();
    }

    @Override
    @Retryable(
            retryFor = TooManyRequestsException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public boolean deleteByName(String name) {
        logger.info("Deleting employee by name: {}", name);

        // Mock server expects {"name": "..."} in request body for DELETE
        Map<String, String> requestBody = Map.of("name", name);

        ApiResponse<Boolean> response = webClient
                .method(org.springframework.http.HttpMethod.DELETE)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        clientResponse -> {
                            if (clientResponse.statusCode().value() == 429) {
                                logger.warn("Rate limited while deleting employee, will retry");
                                return clientResponse
                                        .bodyToMono(String.class)
                                        .map(body -> new TooManyRequestsException("Rate limited: " + body));
                            }
                            return clientResponse
                                    .bodyToMono(String.class)
                                    .map(body -> new RuntimeException("Client error: " + body));
                        })
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        clientResponse -> clientResponse
                                .bodyToMono(String.class)
                                .map(body -> new RuntimeException("Server error: " + body)))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {})
                .block();

        return response != null && Boolean.TRUE.equals(response.data());
    }
}
