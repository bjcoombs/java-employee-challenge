package com.reliaquest.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.domain.EmployeeService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    private EmployeeController controller;

    private static final UUID EMPLOYEE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {
        controller = new EmployeeController(employeeService);
    }

    private Employee createTestEmployee() {
        return new Employee(EMPLOYEE_ID, "John Doe", 50000, 30, "Developer", "john@example.com");
    }

    @Test
    void getAllEmployees_returnsListOfEmployees() {
        List<Employee> employees = List.of(createTestEmployee());
        when(employeeService.getAllEmployees()).thenReturn(employees);

        ResponseEntity<List<Employee>> response = controller.getAllEmployees();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).name()).isEqualTo("John Doe");
    }

    @Test
    void getAllEmployees_returnsEmptyList() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        ResponseEntity<List<Employee>> response = controller.getAllEmployees();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getEmployeesByNameSearch_returnsMatchingEmployees() {
        List<Employee> employees = List.of(createTestEmployee());
        when(employeeService.searchByName("John")).thenReturn(employees);

        ResponseEntity<List<Employee>> response = controller.getEmployeesByNameSearch("John");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).name()).isEqualTo("John Doe");
    }

    @Test
    void getEmployeeById_returnsEmployee() {
        when(employeeService.getById(EMPLOYEE_ID)).thenReturn(createTestEmployee());

        ResponseEntity<Employee> response = controller.getEmployeeById(EMPLOYEE_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(EMPLOYEE_ID);
        assertThat(response.getBody().name()).isEqualTo("John Doe");
    }

    @Test
    void getHighestSalaryOfEmployees_returnsHighestSalary() {
        when(employeeService.getHighestSalary()).thenReturn(100000);

        ResponseEntity<Integer> response = controller.getHighestSalaryOfEmployees();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(100000);
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_returnsNames() {
        List<String> names = List.of("Alice", "Bob", "Charlie");
        when(employeeService.getTopTenHighestEarningNames()).thenReturn(names);

        ResponseEntity<List<String>> response = controller.getTopTenHighestEarningEmployeeNames();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("Alice", "Bob", "Charlie");
    }

    @Test
    void createEmployee_returnsCreatedEmployee() {
        CreateEmployeeRequest request = new CreateEmployeeRequest("John Doe", 50000, 30, "Developer");
        when(employeeService.create(any(CreateEmployeeRequest.class))).thenReturn(createTestEmployee());

        ResponseEntity<Employee> response = controller.createEmployee(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("John Doe");
        verify(employeeService).create(request);
    }

    @Test
    void deleteEmployeeById_returnsDeletedEmployeeName() {
        when(employeeService.deleteById(EMPLOYEE_ID)).thenReturn("John Doe");

        ResponseEntity<String> response = controller.deleteEmployeeById(EMPLOYEE_ID.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("John Doe");
    }
}
