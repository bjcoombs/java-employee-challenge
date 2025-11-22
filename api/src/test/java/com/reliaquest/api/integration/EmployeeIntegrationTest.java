package com.reliaquest.api.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeIntegrationTest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private CacheManager cacheManager;

    private WebTestClient webTestClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        // Note: Not using static WireMock.configureFor() to avoid conflicts with other test classes
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "employee.client.base-url",
                () -> "http://localhost:" + wireMockServer.port() + "/api/v1/employee");
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        // Clear the cache to ensure test isolation
        Objects.requireNonNull(cacheManager.getCache("employees")).clear();
        // Create WebTestClient with increased timeout for retry tests
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    // Helper to load JSON fixture from resources
    private String loadFixture(String filename) throws Exception {
        return Files.readString(
                Paths.get(Objects.requireNonNull(getClass().getResource(filename)).toURI()));
    }

    // GET /api/v1/employee - Get all employees
    @Test
    void getAllEmployees_shouldReturnEmployeeList() throws Exception {
        UUID id = UUID.randomUUID();
        String mockResponse = loadFixture("employee-single.json").formatted(id);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(1)
                .value(employees -> assertThat(employees.getFirst().name()).isEqualTo("John Doe"));
    }

    @Test
    void getAllEmployees_shouldReturnEmptyList_whenNoEmployees() throws Exception {
        String mockResponse = loadFixture("employee-empty-list.json");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(0);
    }

    // GET /api/v1/employee/search/{searchString} - Search by name
    @Test
    void searchByName_shouldReturnMatchingEmployees() throws Exception {
        UUID id = UUID.randomUUID();
        String mockResponse = loadFixture("employee-single.json").formatted(id);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/search/john")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(1);
    }

    @Test
    void searchByName_shouldBeCaseInsensitive() throws Exception {
        UUID id = UUID.randomUUID();
        String mockResponse = loadFixture("employee-single.json").formatted(id);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        // Search with uppercase should find lowercase match
        webTestClient
                .get()
                .uri("/api/v1/employee/search/JOHN")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(1)
                .value(employees -> assertThat(employees.getFirst().name()).isEqualTo("John Doe"));
    }

    // GET /api/v1/employee/{id} - Get employee by ID
    @Test
    void getEmployeeById_shouldReturnEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        String mockResponse = loadFixture("employee-single.json").formatted(id);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/" + id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Employee.class)
                .value(employee -> assertThat(employee.name()).isEqualTo("John Doe"));
    }

    @Test
    void getEmployeeById_shouldReturn404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        String mockResponse = loadFixture("employee-empty-list.json");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient.get().uri("/api/v1/employee/" + id).exchange().expectStatus().isNotFound();
    }

    // GET /api/v1/employee/highestSalary - Get highest salary
    @Test
    void getHighestSalary_shouldReturnHighestSalary() throws Exception {
        String mockResponse = loadFixture("employees-two.json").formatted(UUID.randomUUID(), UUID.randomUUID());

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/highestSalary")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Integer.class)
                .isEqualTo(80000);
    }

    @Test
    void getHighestSalary_shouldReturn0_whenNoEmployees() throws Exception {
        String mockResponse = loadFixture("employee-empty-list.json");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/highestSalary")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Integer.class)
                .isEqualTo(0);
    }

    // GET /api/v1/employee/topTenHighestEarningEmployeeNames - Get top 10 names
    @Test
    void getTopTenHighestEarningEmployeeNames_shouldReturnNames() throws Exception {
        String mockResponse = loadFixture("employees-two.json").formatted(UUID.randomUUID(), UUID.randomUUID());

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/topTenHighestEarningEmployeeNames")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<String>>() {})
                .value(names -> {
                    // Jane Smith (80k) should be first, John Doe (50k) second
                    assertThat(names).containsExactly("Jane Smith", "John Doe");
                });
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_shouldLimitToTen_whenMoreThanTenEmployees() {
        // Create 12 employees with different salaries
        StringBuilder dataBuilder = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            if (i > 1) dataBuilder.append(",");
            dataBuilder.append(
                    """
                    {
                        "id": "%s",
                        "employee_name": "Employee %d",
                        "employee_salary": %d,
                        "employee_age": 30,
                        "employee_title": "Developer",
                        "employee_email": "emp%d@test.com"
                    }
                    """
                            .formatted(UUID.randomUUID(), i, i * 10000, i));
        }

        String mockResponse =
                """
                {
                    "data": [%s],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(dataBuilder.toString());

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/topTenHighestEarningEmployeeNames")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<String>>() {})
                .value(names -> {
                    // Should only return top 10, highest salary first
                    assertThat(names).hasSize(10);
                    // Employee 12 (120k) should be first, Employee 3 (30k) should be last
                    assertThat(names.getFirst()).isEqualTo("Employee 12");
                    assertThat(names.getLast()).isEqualTo("Employee 3");
                });
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_shouldReturnEmptyList_whenNoEmployees() throws Exception {
        String mockResponse = loadFixture("employee-empty-list.json");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/topTenHighestEarningEmployeeNames")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<String>>() {})
                .value(names -> assertThat(names).isEmpty());
    }

    // POST /api/v1/employee - Create employee
    @Test
    void createEmployee_shouldReturnCreatedEmployee() throws Exception {
        UUID id = UUID.randomUUID();
        String mockResponse = loadFixture("employee-create-success.json").formatted(id);

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        var request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Employee.class)
                .value(employee -> assertThat(employee.name()).isEqualTo("New Employee"));
    }

    @Test
    void createEmployee_shouldReturn400_whenInvalidInput() {
        var request = new CreateEmployeeRequest("", -1, 10, ""); // Invalid data

        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    // DELETE /api/v1/employee/{id} - Delete employee
    @Test
    void deleteEmployee_shouldReturnDeletedEmployeeName() throws Exception {
        UUID id = UUID.randomUUID();
        String getAllResponse = loadFixture("employee-single.json").formatted(id);
        String deleteResponse = loadFixture("delete-success.json");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(getAllResponse)));
        wireMockServer.stubFor(delete(urlEqualTo("/api/v1/employee")).willReturn(okJson(deleteResponse)));

        webTestClient
                .delete()
                .uri("/api/v1/employee/" + id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo("John Doe");
    }

    @Test
    void deleteEmployee_shouldReturn404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        String mockResponse = loadFixture("employee-empty-list.json");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient.delete().uri("/api/v1/employee/" + id).exchange().expectStatus().isNotFound();
    }

    // Retry behavior tests
    @Test
    void getAllEmployees_shouldRetryOn429_andEventuallySucceed() throws Exception {
        String successResponse = loadFixture("employee-empty-list.json");

        // First two requests return 429, third succeeds
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withBody("Rate limited"))
                .willSetStateTo("first-retry"));

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("retry")
                .whenScenarioStateIs("first-retry")
                .willReturn(aResponse().withStatus(429).withBody("Rate limited"))
                .willSetStateTo("second-retry"));

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("retry")
                .whenScenarioStateIs("second-retry")
                .willReturn(okJson(successResponse)));

        webTestClient.get().uri("/api/v1/employee").exchange().expectStatus().isOk();

        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void createEmployee_shouldRetryOn429_andEventuallySucceed() throws Exception {
        UUID id = UUID.randomUUID();
        String successResponse = loadFixture("employee-create-success.json").formatted(id);

        // First request returns 429, second succeeds
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee"))
                .inScenario("create-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withBody("Rate limited"))
                .willSetStateTo("retry-once"));

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee"))
                .inScenario("create-retry")
                .whenScenarioStateIs("retry-once")
                .willReturn(okJson(successResponse)));

        var request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated();

        wireMockServer.verify(2, postRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void getAllEmployees_shouldReturn503WithRetryAfter_whenAllRetriesExhausted() {
        // All requests return 429
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(429).withBody("Rate limited")));

        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectHeader()
                .exists("Retry-After");

        // Verify all retry attempts were made (3 attempts)
        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void createEmployee_shouldReturn503WithRetryAfter_whenAllRetriesExhausted() {
        // All requests return 429
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(429).withBody("Rate limited")));

        var request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectHeader()
                .exists("Retry-After");

        // Verify all retry attempts were made
        wireMockServer.verify(3, postRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void deleteEmployee_shouldReturn503WithRetryAfter_whenAllRetriesExhausted() throws Exception {
        UUID id = UUID.randomUUID();
        String getAllResponse = loadFixture("employee-single.json").formatted(id);

        // GET succeeds but DELETE always returns 429
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(getAllResponse)));
        wireMockServer.stubFor(delete(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(429).withBody("Rate limited")));

        webTestClient
                .delete()
                .uri("/api/v1/employee/" + id)
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectHeader()
                .exists("Retry-After");

        // Verify all retry attempts were made for delete
        wireMockServer.verify(3, deleteRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    // 5xx retry behavior tests
    @Test
    void getAllEmployees_shouldRetryOn500_andEventuallySucceed() throws Exception {
        String successResponse = loadFixture("employee-empty-list.json");

        // First two requests return 500, third succeeds
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("5xx-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500).withBody("Internal server error"))
                .willSetStateTo("first-retry"));

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("5xx-retry")
                .whenScenarioStateIs("first-retry")
                .willReturn(aResponse().withStatus(503).withBody("Service unavailable"))
                .willSetStateTo("second-retry"));

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("5xx-retry")
                .whenScenarioStateIs("second-retry")
                .willReturn(okJson(successResponse)));

        webTestClient.get().uri("/api/v1/employee").exchange().expectStatus().isOk();

        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void getAllEmployees_shouldReturn503WithRetryAfter_whenAll5xxRetriesExhausted() {
        // All requests return 500
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(500).withBody("Internal server error")));

        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectHeader()
                .exists("Retry-After");

        // Verify all retry attempts were made (3 attempts)
        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void createEmployee_shouldRetryOn500_andEventuallySucceed() throws Exception {
        UUID id = UUID.randomUUID();
        String successResponse = loadFixture("employee-create-success.json").formatted(id);

        // First request returns 500, second succeeds
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee"))
                .inScenario("create-5xx-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500).withBody("Internal server error"))
                .willSetStateTo("retry-once"));

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee"))
                .inScenario("create-5xx-retry")
                .whenScenarioStateIs("retry-once")
                .willReturn(okJson(successResponse)));

        var request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated();

        wireMockServer.verify(2, postRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void createEmployee_shouldReturn503WithRetryAfter_whenAll5xxRetriesExhausted() {
        // All requests return 502
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(502).withBody("Bad gateway")));

        var request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectHeader()
                .exists("Retry-After");

        // Verify all retry attempts were made
        wireMockServer.verify(3, postRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void deleteEmployee_shouldRetryOn500_andEventuallySucceed() throws Exception {
        UUID id = UUID.randomUUID();
        String getAllResponse = loadFixture("employee-single.json").formatted(id);
        String deleteResponse = loadFixture("delete-success.json");

        // GET succeeds
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(getAllResponse)));

        // First DELETE returns 500, second succeeds
        wireMockServer.stubFor(delete(urlEqualTo("/api/v1/employee"))
                .inScenario("delete-5xx-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500).withBody("Internal server error"))
                .willSetStateTo("retry-once"));

        wireMockServer.stubFor(delete(urlEqualTo("/api/v1/employee"))
                .inScenario("delete-5xx-retry")
                .whenScenarioStateIs("retry-once")
                .willReturn(okJson(deleteResponse)));

        webTestClient
                .delete()
                .uri("/api/v1/employee/" + id)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo("John Doe");

        wireMockServer.verify(2, deleteRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void deleteEmployee_shouldReturn503WithRetryAfter_whenAll5xxRetriesExhausted() throws Exception {
        UUID id = UUID.randomUUID();
        String getAllResponse = loadFixture("employee-single.json").formatted(id);

        // GET succeeds but DELETE always returns 503
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(getAllResponse)));
        wireMockServer.stubFor(delete(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(503).withBody("Service unavailable")));

        webTestClient
                .delete()
                .uri("/api/v1/employee/" + id)
                .exchange()
                .expectStatus()
                .isEqualTo(503)
                .expectHeader()
                .exists("Retry-After");

        // Verify all retry attempts were made for delete
        wireMockServer.verify(3, deleteRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    // Edge case tests
    @Test
    void getEmployeeById_shouldReturn400_whenInvalidUuidFormat() {
        webTestClient
                .get()
                .uri("/api/v1/employee/not-a-valid-uuid")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void searchByName_shouldReturnEmptyList_whenNoMatch() throws Exception {
        String mockResponse = loadFixture("employee-single.json").formatted(UUID.randomUUID());

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/search/nonexistent")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(0);
    }

    // Correlation ID propagation tests
    @Test
    void request_shouldReturnProvidedCorrelationId() throws Exception {
        String mockResponse = loadFixture("employee-empty-list.json");
        String correlationId = "test-correlation-id-12345";

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee")
                .header("X-Correlation-ID", correlationId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("X-Correlation-ID", correlationId);
    }

    @Test
    void request_shouldGenerateCorrelationId_whenNotProvided() throws Exception {
        String mockResponse = loadFixture("employee-empty-list.json");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .exists("X-Correlation-ID")
                .expectHeader()
                .value("X-Correlation-ID", value -> assertThat(value).isNotBlank());
    }

    // Timeout behavior tests
    @Test
    void getAllEmployees_shouldReturn503_whenReadTimeoutExceeded() {
        // Configure WireMock to delay longer than the read timeout (10 seconds default)
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"data\": [], \"status\": \"success\"}")
                        .withFixedDelay(15000))); // 15 second delay

        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }

    // Concurrent request tests
    @Test
    void getAllEmployees_shouldHandleConcurrentRequests() throws Exception {
        String mockResponse = loadFixture("employee-empty-list.json");
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        // Execute multiple concurrent requests
        var results = java.util.concurrent.Executors.newFixedThreadPool(5).invokeAll(
                java.util.stream.IntStream.range(0, 10)
                        .mapToObj(i -> (java.util.concurrent.Callable<Boolean>) () -> {
                            webTestClient
                                    .get()
                                    .uri("/api/v1/employee")
                                    .exchange()
                                    .expectStatus()
                                    .isOk();
                            return true;
                        })
                        .toList());

        // Verify all requests succeeded
        for (var result : results) {
            assertThat(result.get()).isTrue();
        }
    }

    // Cache eviction verification
    @Test
    void cache_shouldBeEvicted_afterCreateEmployee() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // First call - returns employee 1
        String firstResponse = loadFixture("employee-single.json").formatted(id1);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(firstResponse)));

        // First request - populates cache
        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(1)
                .value(employees -> assertThat(employees.getFirst().name()).isEqualTo("John Doe"));

        // Setup create response
        String createResponse = loadFixture("employee-create-success.json").formatted(id2);

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee")).willReturn(okJson(createResponse)));

        // Create employee - should evict cache
        var request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");
        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated();

        // Update mock to return both employees
        String secondResponse = loadFixture("employees-two.json").formatted(id1, id2);

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(secondResponse)));

        // Second request - should get fresh data (not cached)
        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(2);
    }

    // Delete race condition simulation
    @Test
    void deleteEmployee_shouldReturn500_whenDeleteFailsDueToRaceCondition() throws Exception {
        UUID id = UUID.randomUUID();

        // First GET returns the employee
        String getResponse = loadFixture("employee-single.json").formatted(id);

        // DELETE returns false (employee already deleted by another process)
        String deleteResponse = loadFixture("delete-false.json");

        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(getResponse)));
        wireMockServer.stubFor(delete(urlEqualTo("/api/v1/employee")).willReturn(okJson(deleteResponse)));

        // Attempt to delete - employee exists but delete fails (simulating race condition)
        // Returns 500 because EmployeeDeletionException is thrown
        webTestClient
                .delete()
                .uri("/api/v1/employee/" + id)
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }

    // Malformed JSON response handling
    @Test
    void getAllEmployees_shouldReturn502_whenMalformedJsonReceived() {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        webTestClient
                .get()
                .uri("/api/v1/employee")
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }

    @Test
    void createEmployee_shouldReturn502_whenMalformedJsonReceived() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not json at all")));

        var request = new CreateEmployeeRequest("New Employee", 55000, 25, "Developer");

        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }
}
