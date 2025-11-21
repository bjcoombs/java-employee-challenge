package com.reliaquest.api.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reliaquest.api.domain.port.EmployeePort;
import com.reliaquest.api.exception.EmployeeDeletionException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeePort employeePort;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeePort);
    }

    @Test
    void getAllEmployees_shouldReturnAllEmployees() {
        var employees = List.of(
                createEmployee("John Doe", 50000),
                createEmployee("Jane Smith", 60000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.getAllEmployees();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(employees);
    }

    @Test
    void getAllEmployees_shouldReturnEmptyListWhenNoEmployees() {
        when(employeePort.findAll()).thenReturn(List.of());

        var result = employeeService.getAllEmployees();

        assertThat(result).isEmpty();
    }

    @Test
    void searchByName_shouldReturnMatchingEmployees() {
        var employees = List.of(
                createEmployee("John Doe", 50000),
                createEmployee("Jane Doe", 60000),
                createEmployee("Bob Smith", 55000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.searchByName("Doe");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Employee::name).containsExactly("John Doe", "Jane Doe");
    }

    @Test
    void searchByName_shouldBeCaseInsensitive() {
        var employees = List.of(createEmployee("John Doe", 50000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.searchByName("doe");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("John Doe");
    }

    @Test
    void searchByName_shouldReturnEmptyListWhenNoMatch() {
        var employees = List.of(createEmployee("John Doe", 50000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.searchByName("Smith");

        assertThat(result).isEmpty();
    }

    @Test
    void searchByName_shouldReturnAllEmployeesWhenSearchStringIsNull() {
        var employees = List.of(createEmployee("John Doe", 50000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.searchByName(null);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchByName_shouldReturnAllEmployeesWhenSearchStringIsBlank() {
        var employees = List.of(createEmployee("John Doe", 50000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.searchByName("   ");

        assertThat(result).hasSize(1);
    }

    @Test
    void searchByName_shouldHandleEmployeeWithNullName() {
        var employees = List.of(
                createEmployee("John Doe", 50000),
                new Employee(UUID.randomUUID(), null, 60000, 30, "Manager", "test@example.com"));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.searchByName("Doe");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("John Doe");
    }

    @Test
    void getById_shouldReturnEmployee() {
        var id = UUID.randomUUID();
        var employee = createEmployee(id, "John Doe", 50000);
        when(employeePort.findById(id)).thenReturn(Optional.of(employee));

        var result = employeeService.getById(id);

        assertThat(result).isEqualTo(employee);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        var id = UUID.randomUUID();
        when(employeePort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(id))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getHighestSalary_shouldReturnHighestSalary() {
        var employees = List.of(
                createEmployee("John Doe", 50000),
                createEmployee("Jane Smith", 80000),
                createEmployee("Bob Wilson", 60000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.getHighestSalary();

        assertThat(result).isEqualTo(80000);
    }

    @Test
    void getHighestSalary_shouldReturnZeroWhenNoEmployees() {
        when(employeePort.findAll()).thenReturn(List.of());

        var result = employeeService.getHighestSalary();

        assertThat(result).isEqualTo(0);
    }

    @Test
    void getHighestSalary_shouldReturnSalaryWhenSingleEmployee() {
        var employees = List.of(createEmployee("John Doe", 50000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.getHighestSalary();

        assertThat(result).isEqualTo(50000);
    }

    @Test
    void getTopTenHighestEarningNames_shouldReturnTopTenByDescendingSalary() {
        var employees = List.of(
                createEmployee("E1", 10000),
                createEmployee("E2", 20000),
                createEmployee("E3", 30000),
                createEmployee("E4", 40000),
                createEmployee("E5", 50000),
                createEmployee("E6", 60000),
                createEmployee("E7", 70000),
                createEmployee("E8", 80000),
                createEmployee("E9", 90000),
                createEmployee("E10", 100000),
                createEmployee("E11", 110000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.getTopTenHighestEarningNames();

        assertThat(result).hasSize(10);
        assertThat(result).containsExactly("E11", "E10", "E9", "E8", "E7", "E6", "E5", "E4", "E3", "E2");
        assertThat(result).doesNotContain("E1");
    }

    @Test
    void getTopTenHighestEarningNames_shouldReturnAllWhenLessThanTen() {
        var employees = List.of(
                createEmployee("John", 50000),
                createEmployee("Jane", 60000),
                createEmployee("Bob", 55000));
        when(employeePort.findAll()).thenReturn(employees);

        var result = employeeService.getTopTenHighestEarningNames();

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("Jane", "Bob", "John");
    }

    @Test
    void getTopTenHighestEarningNames_shouldReturnEmptyWhenNoEmployees() {
        when(employeePort.findAll()).thenReturn(List.of());

        var result = employeeService.getTopTenHighestEarningNames();

        assertThat(result).isEmpty();
    }

    @Test
    void create_shouldDelegateToPort() {
        var request = new CreateEmployeeRequest("John Doe", 50000, 30, "Developer");
        var createdEmployee = createEmployee("John Doe", 50000);
        when(employeePort.create(request)).thenReturn(createdEmployee);

        var result = employeeService.create(request);

        assertThat(result).isEqualTo(createdEmployee);
        verify(employeePort).create(request);
    }

    @Test
    void deleteById_shouldReturnDeletedEmployeeName() {
        var id = UUID.randomUUID();
        var employee = createEmployee(id, "John Doe", 50000);
        when(employeePort.findById(id)).thenReturn(Optional.of(employee));
        when(employeePort.deleteByName("John Doe")).thenReturn(true);

        var result = employeeService.deleteById(id);

        assertThat(result).isEqualTo("John Doe");
        verify(employeePort).deleteByName("John Doe");
    }

    @Test
    void deleteById_shouldThrowWhenEmployeeNotFound() {
        var id = UUID.randomUUID();
        when(employeePort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.deleteById(id))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void deleteById_shouldThrowWhenDeleteFails() {
        var id = UUID.randomUUID();
        var employee = createEmployee(id, "John Doe", 50000);
        when(employeePort.findById(id)).thenReturn(Optional.of(employee));
        when(employeePort.deleteByName("John Doe")).thenReturn(false);

        assertThatThrownBy(() -> employeeService.deleteById(id))
                .isInstanceOf(EmployeeDeletionException.class)
                .hasMessageContaining("Failed to delete employee");
    }

    private Employee createEmployee(String name, int salary) {
        return createEmployee(UUID.randomUUID(), name, salary);
    }

    private Employee createEmployee(UUID id, String name, int salary) {
        return new Employee(id, name, salary, 30, "Developer", name.toLowerCase().replace(" ", ".") + "@example.com");
    }
}
