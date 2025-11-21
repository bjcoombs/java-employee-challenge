package com.reliaquest.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "employee.cache")
public record CacheProperties(int ttlSeconds, int maxSize) {

    public CacheProperties {
        if (ttlSeconds <= 0) {
            ttlSeconds = 30;
        }
        if (maxSize <= 0) {
            maxSize = 100;
        }
    }
}
