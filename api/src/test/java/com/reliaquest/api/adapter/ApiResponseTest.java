package com.reliaquest.api.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateSuccessResponse() {
        var response = ApiResponse.success("test data");

        assertThat(response.data()).isEqualTo("test data");
        assertThat(response.status()).isEqualTo("Successfully processed request.");
        assertThat(response.error()).isNull();
    }

    @Test
    void shouldCreateErrorResponse() {
        var response = ApiResponse.error("Something went wrong");

        assertThat(response.data()).isNull();
        assertThat(response.status()).isEqualTo("Failed to process request.");
        assertThat(response.error()).isEqualTo("Something went wrong");
    }

    @Test
    void shouldExcludeNullFieldsInJson() throws Exception {
        var response = ApiResponse.success("test");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"data\":\"test\"");
        assertThat(json).contains("\"status\":\"Successfully processed request.\"");
        assertThat(json).doesNotContain("\"error\"");
    }

    @Test
    void shouldIncludeErrorFieldWhenPresent() throws Exception {
        var response = ApiResponse.error("Error message");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"error\":\"Error message\"");
        assertThat(json).doesNotContain("\"data\"");
    }
}
