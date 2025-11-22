package com.reliaquest.api.exception;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final int retryAfterSeconds;

    public GlobalExceptionHandler(@Value("${api.retry-after-seconds:5}") int retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EmployeeNotFoundException ex) {
        logger.warn("Employee not found correlationId={}: {}", getCorrelationId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(EmployeeServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(EmployeeServiceException ex) {
        HttpStatus status = ex.getStatus();
        logger.error("Service error (status {}) correlationId={}: {}", status.value(), getCorrelationId(), ex.getMessage());

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            builder.header("Retry-After", String.valueOf(retryAfterSeconds));
        }

        return builder.body(ErrorResponse.of(status.value(), status.getReasonPhrase(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        logger.warn("Bad request correlationId={}: {}", getCorrelationId(), ex.getMessage());
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors();
        String message = fieldErrors.isEmpty()
                ? "Validation failed"
                : fieldErrors.stream()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        logger.warn("Validation failed correlationId={}: {}", getCorrelationId(), message);
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Bad Request", message));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex) {
        logger.warn("Too many requests correlationId={}: {}", getCorrelationId(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(ErrorResponse.of(429, "Too Many Requests", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        logger.error("Unexpected error correlationId={}", getCorrelationId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : "unknown";
    }
}
