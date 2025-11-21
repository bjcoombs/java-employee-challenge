package com.reliaquest.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
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

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("error", "Not Found");
        assertThat(response.getBody()).containsKey("message");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleDeletionFailure_shouldReturn500WithMessage() {
        var exception = new EmployeeDeletionException("Failed to delete employee");

        ResponseEntity<Map<String, Object>> response = handler.handleDeletionFailure(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(response.getBody()).containsEntry("message", "Failed to delete employee");
    }

    @Test
    void handleBadRequest_shouldReturn400WithMessage() {
        var exception = new IllegalArgumentException("Invalid argument");

        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Bad Request");
        assertThat(response.getBody()).containsEntry("message", "Invalid argument");
    }

    @Test
    void handleValidation_shouldReturn400WithFieldError() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("error", "Bad Request");
        assertThat(response.getBody()).containsEntry("message", "name: must not be blank");
    }

    @Test
    void handleExternalService_shouldReturn502WithMessage() {
        var exception = new ExternalServiceException("Connection refused", 500);

        ResponseEntity<Map<String, Object>> response = handler.handleExternalService(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("status", 502);
        assertThat(response.getBody()).containsEntry("error", "Bad Gateway");
        assertThat(response.getBody().get("message").toString()).contains("Connection refused");
    }

    @Test
    void handleServiceUnavailable_shouldReturn503WithRetryAfterHeader() {
        var exception = new ServiceUnavailableException("Service temporarily unavailable");

        ResponseEntity<Map<String, Object>> response = handler.handleServiceUnavailable(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
        assertThat(response.getBody()).containsEntry("status", 503);
        assertThat(response.getBody()).containsEntry("error", "Service Unavailable");
        assertThat(response.getBody()).containsEntry("message", "Service temporarily unavailable");
    }

    @Test
    void handleTooManyRequests_shouldReturn429WithRetryAfterHeader() {
        var exception = new TooManyRequestsException("Rate limited");

        ResponseEntity<Map<String, Object>> response = handler.handleTooManyRequests(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
        assertThat(response.getBody()).containsEntry("status", 429);
        assertThat(response.getBody()).containsEntry("error", "Too Many Requests");
        assertThat(response.getBody()).containsEntry("message", "Rate limited");
    }

    @Test
    void handleGeneric_shouldReturn500WithGenericMessage() {
        var exception = new RuntimeException("Something unexpected");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("error", "Internal Server Error");
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred");
    }

    @Test
    void errorBody_shouldIncludeTimestamp() {
        var exception = new EmployeeNotFoundException(UUID.randomUUID());

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(exception);

        assertThat(response.getBody().get("timestamp")).isNotNull();
        assertThat(response.getBody().get("timestamp").toString()).matches("\\d{4}-\\d{2}-\\d{2}T.*");
    }
}
