package com.reliaquest.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.reliaquest.api.exception.EmployeeDeletionException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.ExternalServiceException;
import com.reliaquest.api.exception.TooManyRequestsException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleEmployeeNotFoundException_returns404() {
        UUID id = UUID.randomUUID();
        var exception = new EmployeeNotFoundException(id);

        ResponseEntity<ErrorResponse> response = handler.handleEmployeeNotFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains(id.toString());
    }

    @Test
    void handleEmployeeDeletionException_returns500() {
        var exception = new EmployeeDeletionException("Failed to delete employee");

        ResponseEntity<ErrorResponse> response = handler.handleEmployeeDeletionException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Failed to delete employee");
    }

    @Test
    void handleTooManyRequestsException_returns429() {
        var exception = new TooManyRequestsException("Rate limited");

        ResponseEntity<ErrorResponse> response = handler.handleTooManyRequestsException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Rate limited");
    }

    @Test
    void handleExternalServiceException_returns502() {
        var exception = new ExternalServiceException("External service unavailable", 503);

        ResponseEntity<ErrorResponse> response = handler.handleExternalServiceException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("External service unavailable");
    }

    @Test
    void handleIllegalArgumentException_returns400() {
        var exception = new IllegalArgumentException("Invalid UUID format");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid UUID format");
    }
}
