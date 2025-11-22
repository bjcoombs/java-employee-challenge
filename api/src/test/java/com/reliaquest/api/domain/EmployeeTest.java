package com.reliaquest.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmployeeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerialize() throws Exception {
        var employee = new Employee(
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                "John Doe",
                50000,
                30,
                "Developer",
                "john@example.com");

        String json = objectMapper.writeValueAsString(employee);

        assertThat(json).contains("\"id\":");
        assertThat(json).contains("\"name\":\"John Doe\"");
        assertThat(json).contains("\"salary\":50000");
        assertThat(json).contains("\"age\":30");
        assertThat(json).contains("\"title\":\"Developer\"");
        assertThat(json).contains("\"email\":\"john@example.com\"");
    }

    @Test
    void shouldDeserialize() throws Exception {
        String json =
                """
                {
                    "id": "123e4567-e89b-12d3-a456-426614174000",
                    "name": "Jane Doe",
                    "salary": 60000,
                    "age": 25,
                    "title": "Manager",
                    "email": "jane@example.com"
                }
                """;

        Employee employee = objectMapper.readValue(json, Employee.class);

        assertThat(employee.id()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        assertThat(employee.name()).isEqualTo("Jane Doe");
        assertThat(employee.salary()).isEqualTo(60000);
        assertThat(employee.age()).isEqualTo(25);
        assertThat(employee.title()).isEqualTo("Manager");
        assertThat(employee.email()).isEqualTo("jane@example.com");
    }
}
