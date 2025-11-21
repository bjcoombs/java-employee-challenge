package com.reliaquest.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "employee.client")
public record EmployeeClientProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        RetryProperties retry) {

    public record RetryProperties(
            int maxAttempts,
            Duration delay,
            double multiplier) {

        public RetryProperties {
            if (maxAttempts <= 0) {
                maxAttempts = 3;
            }
            if (delay == null) {
                delay = Duration.ofMillis(500);
            }
            if (multiplier <= 0) {
                multiplier = 2.0;
            }
        }
    }

    public EmployeeClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8112/api/v1/employee";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(10);
        }
        if (retry == null) {
            retry = new RetryProperties(3, Duration.ofMillis(500), 2.0);
        }
    }
}
