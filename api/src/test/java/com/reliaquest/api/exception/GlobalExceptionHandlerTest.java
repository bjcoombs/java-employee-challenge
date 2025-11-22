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
        handler = new GlobalExceptionHandler(5);
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
    void handleServiceException_shouldReturn500ForInternalServerError() {
        var exception = new EmployeeServiceException("Failed to delete employee", HttpStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("Failed to delete employee");
    }

    @Test
    void handleServiceException_shouldReturn502ForBadGateway() {
        var exception = new EmployeeServiceException("Connection refused", HttpStatus.BAD_GATEWAY);

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().status()).isEqualTo(502);
        assertThat(response.getBody().error()).isEqualTo("Bad Gateway");
        assertThat(response.getBody().message()).isEqualTo("Connection refused");
    }

    @Test
    void handleServiceException_shouldReturn503WithRetryAfterHeader() {
        var exception = new EmployeeServiceException("Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);

        ResponseEntity<ErrorResponse> response = handler.handleServiceException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().error()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().message()).isEqualTo("Service temporarily unavailable");
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
