package com.reliaquest.api.domain;

import java.util.UUID;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

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
