package com.reliaquest.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_shouldReturn404WithMessage() {
        var exception = new EmployeeNotFoundException(UUID.randomUUID());

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void handleDeletionFailure_shouldReturn500WithMessage() {
        var exception = new EmployeeDeletionException("Failed to delete employee");

        ResponseEntity<ErrorResponse> response = handler.handleDeletionFailure(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("Failed to delete employee");
    }

    @Test
    void handleBadRequest_shouldReturn400WithMessage() {
        var exception = new IllegalArgumentException("Invalid argument");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid argument");
    }

    @Test
    void handleValidation_shouldReturn400WithAllFieldErrors() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        bindingResult.addError(new FieldError("request", "salary", "must be positive"));
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).contains("name: must not be blank").contains("salary: must be positive");
    }

    @Test
    void handleValidation_shouldReturn400WithDefaultMessageWhenNoFieldErrors() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
    }

    @Test
    void handleExternalService_shouldReturn502WithStatusCodeAndMessage() {
        var exception = new ExternalServiceException("Connection refused", 500);

        ResponseEntity<ErrorResponse> response = handler.handleExternalService(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().status()).isEqualTo(502);
        assertThat(response.getBody().error()).isEqualTo("Bad Gateway");
        assertThat(response.getBody().message()).contains("status 500").contains("Connection refused");
    }

    @Test
    void handleServiceUnavailable_shouldReturn503WithRetryAfterHeader() {
        var exception = new ServiceUnavailableException("Service temporarily unavailable");

        ResponseEntity<ErrorResponse> response = handler.handleServiceUnavailable(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().error()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().message()).isEqualTo("Service temporarily unavailable");
    }

    @Test
    void handleTooManyRequests_shouldReturn429WithRetryAfterHeader() {
        var exception = new TooManyRequestsException("Rate limited");

        ResponseEntity<ErrorResponse> response = handler.handleTooManyRequests(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
        assertThat(response.getBody().status()).isEqualTo(429);
        assertThat(response.getBody().error()).isEqualTo("Too Many Requests");
        assertThat(response.getBody().message()).isEqualTo("Rate limited");
    }

    @Test
    void handleGeneric_shouldReturn500WithGenericMessage() {
        var exception = new RuntimeException("Something unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void errorResponse_shouldIncludeTimestamp() {
        var exception = new EmployeeNotFoundException(UUID.randomUUID());

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception);

        assertThat(response.getBody().timestamp()).isNotNull();
    }
}
