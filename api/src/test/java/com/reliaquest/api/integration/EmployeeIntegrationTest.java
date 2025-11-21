package com.reliaquest.api.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.reliaquest.api.domain.Employee;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeIntegrationTest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8112));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8112);
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
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

    // GET /api/v1/employee - Get all employees
    @Test
    void getAllEmployees_shouldReturnEmployeeList() {
        UUID id = UUID.randomUUID();
        String mockResponse =
                """
                {
                    "data": [
                        {
                            "id": "%s",
                            "employee_name": "John Doe",
                            "employee_salary": 50000,
                            "employee_age": 30,
                            "employee_title": "Developer",
                            "employee_email": "john@test.com"
                        }
                    ],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(id);

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

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
    void getAllEmployees_shouldReturnEmptyList_whenNoEmployees() {
        String mockResponse =
                """
                {
                    "data": [],
                    "status": "Successfully processed request."
                }
                """;

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

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
    void searchByName_shouldReturnMatchingEmployees() {
        UUID id = UUID.randomUUID();
        String mockResponse =
                """
                {
                    "data": [
                        {
                            "id": "%s",
                            "employee_name": "John Doe",
                            "employee_salary": 50000,
                            "employee_age": 30,
                            "employee_title": "Developer",
                            "employee_email": "john@test.com"
                        }
                    ],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(id);

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/search/john")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(1);
    }

    // GET /api/v1/employee/{id} - Get employee by ID
    @Test
    void getEmployeeById_shouldReturnEmployee() {
        UUID id = UUID.randomUUID();
        String mockResponse =
                """
                {
                    "data": [
                        {
                            "id": "%s",
                            "employee_name": "John Doe",
                            "employee_salary": 50000,
                            "employee_age": 30,
                            "employee_title": "Developer",
                            "employee_email": "john@test.com"
                        }
                    ],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(id);

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

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
    void getEmployeeById_shouldReturn404_whenNotFound() {
        UUID id = UUID.randomUUID();
        String mockResponse =
                """
                {
                    "data": [],
                    "status": "Successfully processed request."
                }
                """;

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient.get().uri("/api/v1/employee/" + id).exchange().expectStatus().isNotFound();
    }

    // GET /api/v1/employee/highestSalary - Get highest salary
    @Test
    void getHighestSalary_shouldReturnHighestSalary() {
        String mockResponse =
                """
                {
                    "data": [
                        {
                            "id": "%s",
                            "employee_name": "John Doe",
                            "employee_salary": 50000,
                            "employee_age": 30,
                            "employee_title": "Developer",
                            "employee_email": "john@test.com"
                        },
                        {
                            "id": "%s",
                            "employee_name": "Jane Smith",
                            "employee_salary": 80000,
                            "employee_age": 35,
                            "employee_title": "Manager",
                            "employee_email": "jane@test.com"
                        }
                    ],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/highestSalary")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Integer.class)
                .isEqualTo(80000);
    }

    // GET /api/v1/employee/topTenHighestEarningEmployeeNames - Get top 10 names
    @Test
    void getTopTenHighestEarningEmployeeNames_shouldReturnNames() {
        String mockResponse =
                """
                {
                    "data": [
                        {
                            "id": "%s",
                            "employee_name": "John Doe",
                            "employee_salary": 50000,
                            "employee_age": 30,
                            "employee_title": "Developer",
                            "employee_email": "john@test.com"
                        },
                        {
                            "id": "%s",
                            "employee_name": "Jane Smith",
                            "employee_salary": 80000,
                            "employee_age": 35,
                            "employee_title": "Manager",
                            "employee_email": "jane@test.com"
                        }
                    ],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(UUID.randomUUID(), UUID.randomUUID());

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/topTenHighestEarningEmployeeNames")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<String>>() {})
                .value(names -> {
                    assertThat(names).contains("Jane Smith", "John Doe");
                });
    }

    // POST /api/v1/employee - Create employee
    @Test
    void createEmployee_shouldReturnCreatedEmployee() {
        UUID id = UUID.randomUUID();
        String mockResponse =
                """
                {
                    "data": {
                        "id": "%s",
                        "employee_name": "New Employee",
                        "employee_salary": 55000,
                        "employee_age": 25,
                        "employee_title": "Developer",
                        "employee_email": "new@test.com"
                    },
                    "status": "Successfully processed request."
                }
                """
                        .formatted(id);

        stubFor(post(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        var request = new CreateEmployeeRequestDto("New Employee", 55000, 25, "Developer");

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
        var request = new CreateEmployeeRequestDto("", -1, 10, ""); // Invalid data

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
    void deleteEmployee_shouldReturnDeletedEmployeeName() {
        UUID id = UUID.randomUUID();
        String getAllResponse =
                """
                {
                    "data": [
                        {
                            "id": "%s",
                            "employee_name": "John Doe",
                            "employee_salary": 50000,
                            "employee_age": 30,
                            "employee_title": "Developer",
                            "employee_email": "john@test.com"
                        }
                    ],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(id);

        String deleteResponse =
                """
                {
                    "data": true,
                    "status": "Successfully processed request."
                }
                """;

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(getAllResponse)));
        stubFor(delete(urlEqualTo("/api/v1/employee")).willReturn(okJson(deleteResponse)));

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
    void deleteEmployee_shouldReturn404_whenNotFound() {
        UUID id = UUID.randomUUID();
        String mockResponse =
                """
                {
                    "data": [],
                    "status": "Successfully processed request."
                }
                """;

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient.delete().uri("/api/v1/employee/" + id).exchange().expectStatus().isNotFound();
    }

    // Retry behavior tests
    @Test
    void getAllEmployees_shouldRetryOn429_andEventuallySucceed() {
        String successResponse =
                """
                {
                    "data": [],
                    "status": "Successfully processed request."
                }
                """;

        // First two requests return 429, third succeeds
        stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withBody("Rate limited"))
                .willSetStateTo("first-retry"));

        stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("retry")
                .whenScenarioStateIs("first-retry")
                .willReturn(aResponse().withStatus(429).withBody("Rate limited"))
                .willSetStateTo("second-retry"));

        stubFor(get(urlEqualTo("/api/v1/employee"))
                .inScenario("retry")
                .whenScenarioStateIs("second-retry")
                .willReturn(okJson(successResponse)));

        webTestClient.get().uri("/api/v1/employee").exchange().expectStatus().isOk();

        verify(3, getRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void createEmployee_shouldRetryOn429_andEventuallySucceed() {
        UUID id = UUID.randomUUID();
        String successResponse =
                """
                {
                    "data": {
                        "id": "%s",
                        "employee_name": "New Employee",
                        "employee_salary": 55000,
                        "employee_age": 25,
                        "employee_title": "Developer",
                        "employee_email": "new@test.com"
                    },
                    "status": "Successfully processed request."
                }
                """
                        .formatted(id);

        // First request returns 429, second succeeds
        stubFor(post(urlEqualTo("/api/v1/employee"))
                .inScenario("create-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withBody("Rate limited"))
                .willSetStateTo("retry-once"));

        stubFor(post(urlEqualTo("/api/v1/employee"))
                .inScenario("create-retry")
                .whenScenarioStateIs("retry-once")
                .willReturn(okJson(successResponse)));

        var request = new CreateEmployeeRequestDto("New Employee", 55000, 25, "Developer");

        webTestClient
                .post()
                .uri("/api/v1/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated();

        verify(2, postRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void getAllEmployees_shouldReturn503WithRetryAfter_whenAllRetriesExhausted() {
        // All requests return 429
        stubFor(get(urlEqualTo("/api/v1/employee"))
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
        verify(3, getRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void createEmployee_shouldReturn503WithRetryAfter_whenAllRetriesExhausted() {
        // All requests return 429
        stubFor(post(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(429).withBody("Rate limited")));

        var request = new CreateEmployeeRequestDto("New Employee", 55000, 25, "Developer");

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
        verify(3, postRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    @Test
    void deleteEmployee_shouldReturn503WithRetryAfter_whenAllRetriesExhausted() {
        UUID id = UUID.randomUUID();
        String getAllResponse =
                """
                {
                    "data": [
                        {
                            "id": "%s",
                            "employee_name": "John Doe",
                            "employee_salary": 50000,
                            "employee_age": 30,
                            "employee_title": "Developer",
                            "employee_email": "john@test.com"
                        }
                    ],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(id);

        // GET succeeds but DELETE always returns 429
        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(getAllResponse)));
        stubFor(delete(urlEqualTo("/api/v1/employee"))
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
        verify(3, deleteRequestedFor(urlEqualTo("/api/v1/employee")));
    }

    // Error handling tests
    @Test
    void getAllEmployees_shouldReturn503_whenExternalServiceFails() {
        stubFor(get(urlEqualTo("/api/v1/employee"))
                .willReturn(aResponse().withStatus(500).withBody("Internal server error")));

        webTestClient.get().uri("/api/v1/employee").exchange().expectStatus().is5xxServerError();
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
    void searchByName_shouldReturnEmptyList_whenNoMatch() {
        String mockResponse =
                """
                {
                    "data": [
                        {
                            "id": "%s",
                            "employee_name": "John Doe",
                            "employee_salary": 50000,
                            "employee_age": 30,
                            "employee_title": "Developer",
                            "employee_email": "john@test.com"
                        }
                    ],
                    "status": "Successfully processed request."
                }
                """
                        .formatted(UUID.randomUUID());

        stubFor(get(urlEqualTo("/api/v1/employee")).willReturn(okJson(mockResponse)));

        webTestClient
                .get()
                .uri("/api/v1/employee/search/nonexistent")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Employee.class)
                .hasSize(0);
    }

    // DTO for request body
    record CreateEmployeeRequestDto(String name, Integer salary, Integer age, String title) {}
}
