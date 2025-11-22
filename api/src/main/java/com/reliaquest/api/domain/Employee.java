package com.reliaquest.api.domain;

import java.util.UUID;

/**
 * Employee data returned from the mock server.
 * Note: Despite MockEmployee.java having @JsonNaming with employee_ prefix,
 * the actual server response uses plain field names (name, salary, etc.)
 */
public record Employee(
        UUID id,
        String name,
        Integer salary,
        Integer age,
        String title,
        String email) {
}
