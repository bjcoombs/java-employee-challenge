package com.reliaquest.api.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(Employee.PrefixNamingStrategy.class)
public record Employee(
        UUID id,
        String name,
        Integer salary,
        Integer age,
        String title,
        String email) {

    static class PrefixNamingStrategy extends PropertyNamingStrategies.NamingBase {

        @Override
        public String translate(String propertyName) {
            if ("id".equals(propertyName)) {
                return propertyName;
            }
            return "employee_" + propertyName;
        }
    }
}
