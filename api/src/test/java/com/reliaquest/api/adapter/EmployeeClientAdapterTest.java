package com.reliaquest.api.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.exception.ExternalServiceException;
import com.reliaquest.api.exception.TooManyRequestsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

class EmployeeClientAdapterTest {

    private WireMockServer wireMockServer;
    private EmployeeClientAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        // Configure WebClient.Builder with Jackson codec for proper annotation support
        ObjectMapper objectMapper = new ObjectMapper();
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                })
                .build();

        WebClient.Builder builder = WebClient.builder().exchangeStrategies(strategies);

        // Create adapter with test URL pointing to WireMock
        adapter = new TestableEmployeeClientAdapter(
                builder, "http://localhost:" + wireMockServer.port() + "/api/v1/employee");
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    // Helper to load JSON fixture from resources
    private String loadFixture(String filename) throws Exception {
        return Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getResource(filename)).toURI()));
    }

    // findAll tests

    @Test
    void findAll_shouldReturnEmployees_whenApiReturnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        String json = String.format(loadFixture("employee-list-success.json"), id);

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        List<Employee> result = adapter.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(id);
        assertThat(result.getFirst().name()).isEqualTo("John Doe");
    }

    @Test
    void findAll_shouldReturnEmptyList_whenApiReturnsNullData() throws Exception {
        String json = loadFixture("employee-null-data.json");

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        List<Employee> result = adapter.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldThrowTooManyRequestsException_when429Received() {
        stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(429).withBody("Rate limited")));

        assertThatThrownBy(() -> adapter.findAll())
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Rate limited");
    }

    @Test
    void findAll_shouldThrowExternalServiceException_whenClientErrorOccurs() {
        stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(400).withBody("Bad request")));

        assertThatThrownBy(() -> adapter.findAll())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Client error");
    }

    // findById tests

    @Test
    void findById_shouldReturnEmployee_whenFound() throws Exception {
        UUID id = UUID.randomUUID();
        String json = String.format(loadFixture("employee-findbyid-success.json"), id);

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        Optional<Employee> result = adapter.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Jane Doe");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() throws Exception {
        UUID searchId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        String json = String.format(loadFixture("employee-findbyid-success.json"), otherId);

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        Optional<Employee> result = adapter.findById(searchId);

        assertThat(result).isEmpty();
    }

    // create tests

    @Test
    void create_shouldReturnEmployee_whenApiReturnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        String json = String.format(loadFixture("employee-create-success.json"), id);

        stubFor(post(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        CreateEmployeeRequest request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");
        Employee result = adapter.create(request);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("New Employee");
    }

    @Test
    void create_shouldThrowException_whenApiReturnsNull() throws Exception {
        String json = loadFixture("employee-null-data.json");

        stubFor(post(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        CreateEmployeeRequest request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        assertThatThrownBy(() -> adapter.create(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Failed to create employee");
    }

    @Test
    void create_shouldThrowTooManyRequestsException_when429Received() {
        stubFor(post(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(429).withBody("Rate limited")));

        CreateEmployeeRequest request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        assertThatThrownBy(() -> adapter.create(request))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Rate limited");
    }

    // deleteByName tests

    @Test
    void deleteByName_shouldReturnTrue_whenDeleteSucceeds() throws Exception {
        String json = loadFixture("delete-success.json");

        stubFor(delete(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        boolean result = adapter.deleteByName("John Doe");

        assertThat(result).isTrue();
        verify(deleteRequestedFor(urlEqualTo("/api/v1/employee"))
                .withRequestBody(containing("\"name\":\"John Doe\"")));
    }

    @Test
    void deleteByName_shouldReturnFalse_whenDeleteFails() throws Exception {
        String json = loadFixture("delete-false.json");

        stubFor(delete(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        boolean result = adapter.deleteByName("Unknown");

        assertThat(result).isFalse();
    }

    @Test
    void deleteByName_shouldReturnFalse_whenResponseIsNull() throws Exception {
        String json = loadFixture("employee-null-data.json");

        stubFor(delete(urlEqualTo("/api/v1/employee")).willReturn(okJson(json)));

        boolean result = adapter.deleteByName("Unknown");

        assertThat(result).isFalse();
    }

    @Test
    void deleteByName_shouldThrowTooManyRequestsException_when429Received() {
        stubFor(delete(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(429).withBody("Rate limited")));

        assertThatThrownBy(() -> adapter.deleteByName("John Doe"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("Rate limited");
    }

    // 5xx error handling tests

    @Test
    void findAll_shouldThrowExternalServiceException_when5xxErrorOccurs() {
        stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(500).withBody("Internal server error")));

        assertThatThrownBy(() -> adapter.findAll())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void create_shouldThrowExternalServiceException_when5xxErrorOccurs() {
        stubFor(post(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(503).withBody("Service unavailable")));

        CreateEmployeeRequest request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        assertThatThrownBy(() -> adapter.create(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void deleteByName_shouldThrowExternalServiceException_when5xxErrorOccurs() {
        stubFor(delete(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(502).withBody("Bad gateway")));

        assertThatThrownBy(() -> adapter.deleteByName("John Doe"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Server error");
    }

    // Testable subclass to allow custom base URL
    private static class TestableEmployeeClientAdapter extends EmployeeClientAdapter {

        TestableEmployeeClientAdapter(WebClient.Builder webClientBuilder, String baseUrl) {
            super(webClientBuilder, baseUrl);
        }
    }
}
