package com.reliaquest.api.exception;

public class RetryableServerException extends RuntimeException {
    private final int statusCode;

    public RetryableServerException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
