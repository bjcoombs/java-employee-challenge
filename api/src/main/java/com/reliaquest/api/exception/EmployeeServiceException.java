package com.reliaquest.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Consolidated exception for employee service errors.
 * Replaces: EmployeeDeletionException, ExternalServiceException, ServiceUnavailableException.
 */
public class EmployeeServiceException extends RuntimeException {

    private final HttpStatus status;

    public EmployeeServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public EmployeeServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getStatusCode() {
        return status.value();
    }
}
