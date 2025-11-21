package com.reliaquest.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.domain.EmployeeService;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.TooManyRequestsException;
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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

    @Test
    void getEmployeeById_invalidUuid_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> controller.getEmployeeById("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid employee ID format: not-a-uuid");
    }

    @Test
    void deleteEmployeeById_invalidUuid_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> controller.deleteEmployeeById("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid employee ID format: invalid-uuid");
    }

    @Test
    void getEmployeeById_notFound_propagatesException() {
        when(employeeService.getById(EMPLOYEE_ID)).thenThrow(new EmployeeNotFoundException(EMPLOYEE_ID));

        assertThatThrownBy(() -> controller.getEmployeeById(EMPLOYEE_ID.toString()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void getAllEmployees_serviceThrowsException_propagates() {
        when(employeeService.getAllEmployees()).thenThrow(new TooManyRequestsException("Rate limited"));

        assertThatThrownBy(() -> controller.getAllEmployees()).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void getEmployeesByNameSearch_serviceThrowsException_propagates() {
        when(employeeService.searchByName(any())).thenThrow(new TooManyRequestsException("Rate limited"));

        assertThatThrownBy(() -> controller.getEmployeesByNameSearch("test"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void getEmployeesByNameSearch_emptyString_delegatesToService() {
        List<Employee> employees = List.of(createTestEmployee());
        when(employeeService.searchByName("")).thenReturn(employees);

        ResponseEntity<List<Employee>> response = controller.getEmployeesByNameSearch("");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getHighestSalaryOfEmployees_noEmployees_returnsZero() {
        when(employeeService.getHighestSalary()).thenReturn(0);

        ResponseEntity<Integer> response = controller.getHighestSalaryOfEmployees();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0);
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_fewerThanTen_returnsAvailable() {
        List<String> names = List.of("Alice", "Bob");
        when(employeeService.getTopTenHighestEarningNames()).thenReturn(names);

        ResponseEntity<List<String>> response = controller.getTopTenHighestEarningEmployeeNames();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).containsExactly("Alice", "Bob");
    }

    @Test
    void getTopTenHighestEarningEmployeeNames_noEmployees_returnsEmptyList() {
        when(employeeService.getTopTenHighestEarningNames()).thenReturn(List.of());

        ResponseEntity<List<String>> response = controller.getTopTenHighestEarningEmployeeNames();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
