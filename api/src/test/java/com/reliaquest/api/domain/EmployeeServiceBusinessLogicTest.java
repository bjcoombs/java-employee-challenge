package com.reliaquest.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for EmployeeService business logic methods.
 * These test filtering, sorting, and aggregation logic in isolation from HTTP concerns.
 */
class EmployeeServiceBusinessLogicTest {

    private TestableEmployeeService employeeService;
    private List<Employee> testEmployees;

    @BeforeEach
    void setUp() {
        testEmployees = List.of(
                new Employee(UUID.randomUUID(), "Alice Johnson", 75000, 30, "Engineer", "alice@test.com"),
                new Employee(UUID.randomUUID(), "Bob Smith", 95000, 45, "Manager", "bob@test.com"),
                new Employee(UUID.randomUUID(), "Charlie Brown", 65000, 25, "Analyst", "charlie@test.com"),
                new Employee(UUID.randomUUID(), "Diana Prince", 120000, 35, "Director", "diana@test.com"),
                new Employee(UUID.randomUUID(), "Eve Wilson", 85000, 40, "Engineer", "eve@test.com"),
                new Employee(UUID.randomUUID(), "Frank Castle", 70000, 50, "Analyst", "frank@test.com"),
                new Employee(UUID.randomUUID(), "Grace Lee", 110000, 38, "Manager", "grace@test.com"),
                new Employee(UUID.randomUUID(), "Henry Ford", 55000, 22, "Intern", "henry@test.com"),
                new Employee(UUID.randomUUID(), "Iris West", 90000, 32, "Engineer", "iris@test.com"),
                new Employee(UUID.randomUUID(), "Jack Ryan", 105000, 42, "Director", "jack@test.com"),
                new Employee(UUID.randomUUID(), "Kate Bishop", 60000, 28, "Analyst", "kate@test.com"),
                new Employee(UUID.randomUUID(), "Liam Neeson", 80000, 55, "Manager", "liam@test.com"));

        employeeService = new TestableEmployeeService(testEmployees);
    }

    // Test helper class that overrides getAllEmployees to return test data
    private static class TestableEmployeeService extends EmployeeService {
        private List<Employee> employees;

        TestableEmployeeService(List<Employee> employees) {
            super();
            this.employees = employees;
        }

        @Override
        public List<Employee> getAllEmployees() {
            return employees;
        }

        void setEmployees(List<Employee> employees) {
            this.employees = employees;
        }
    }

    // searchByName tests

    @Test
    void searchByName_shouldReturnMatchingEmployees_caseInsensitive() {
        List<Employee> results = employeeService.searchByName("alice");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Alice Johnson");
    }

    @Test
    void searchByName_shouldReturnMultipleMatches() {
        List<Employee> results = employeeService.searchByName("e");
        // Alice, Charlie, Eve, Grace, Henry, Iris, Kate, Neeson, West all contain 'e'
        assertThat(results).hasSizeGreaterThan(1);
        assertThat(results).extracting(Employee::name).contains("Alice Johnson", "Eve Wilson", "Grace Lee");
    }

    @Test
    void searchByName_shouldReturnEmptyList_whenNoMatch() {
        List<Employee> results = employeeService.searchByName("xyz");
        assertThat(results).isEmpty();
    }

    @Test
    void searchByName_shouldReturnAllEmployees_whenSearchStringIsNull() {
        List<Employee> results = employeeService.searchByName(null);
        assertThat(results).hasSize(12);
    }

    @Test
    void searchByName_shouldReturnAllEmployees_whenSearchStringIsBlank() {
        List<Employee> results = employeeService.searchByName("   ");
        assertThat(results).hasSize(12);
    }

    @Test
    void searchByName_shouldHandleUpperCaseSearch() {
        List<Employee> results = employeeService.searchByName("ALICE");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Alice Johnson");
    }

    @Test
    void searchByName_shouldHandleMixedCaseSearch() {
        List<Employee> results = employeeService.searchByName("AlIcE");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Alice Johnson");
    }

    // getHighestSalary tests

    @Test
    void getHighestSalary_shouldReturnHighestValue() {
        int highest = employeeService.getHighestSalary();
        assertThat(highest).isEqualTo(120000); // Diana Prince
    }

    @Test
    void getHighestSalary_shouldReturn0_whenNoEmployees() {
        employeeService.setEmployees(List.of());
        int highest = employeeService.getHighestSalary();
        assertThat(highest).isEqualTo(0);
    }

    @Test
    void getHighestSalary_shouldHandleNullSalaries() {
        List<Employee> employeesWithNull = List.of(
                new Employee(UUID.randomUUID(), "Test", null, 30, "Engineer", "test@test.com"),
                new Employee(UUID.randomUUID(), "Test2", 50000, 25, "Analyst", "test2@test.com"));
        employeeService.setEmployees(employeesWithNull);

        int highest = employeeService.getHighestSalary();
        assertThat(highest).isEqualTo(50000);
    }

    @Test
    void getHighestSalary_shouldReturn0_whenAllSalariesNull() {
        List<Employee> employeesWithNull = List.of(
                new Employee(UUID.randomUUID(), "Test", null, 30, "Engineer", "test@test.com"),
                new Employee(UUID.randomUUID(), "Test2", null, 25, "Analyst", "test2@test.com"));
        employeeService.setEmployees(employeesWithNull);

        int highest = employeeService.getHighestSalary();
        assertThat(highest).isEqualTo(0);
    }

    // getTopTenHighestEarningNames tests

    @Test
    void getTopTenHighestEarningNames_shouldReturnTop10SortedByDescendingSalary() {
        List<String> topTen = employeeService.getTopTenHighestEarningNames();

        assertThat(topTen).hasSize(10);
        // Verify order: Diana (120k), Grace (110k), Jack (105k), Bob (95k), Iris (90k),
        // Eve (85k), Liam (80k), Alice (75k), Frank (70k), Charlie (65k)
        assertThat(topTen)
                .containsExactly(
                        "Diana Prince",
                        "Grace Lee",
                        "Jack Ryan",
                        "Bob Smith",
                        "Iris West",
                        "Eve Wilson",
                        "Liam Neeson",
                        "Alice Johnson",
                        "Frank Castle",
                        "Charlie Brown");
    }

    @Test
    void getTopTenHighestEarningNames_shouldReturnAllNames_whenFewerThan10Employees() {
        employeeService.setEmployees(testEmployees.subList(0, 5));

        List<String> topTen = employeeService.getTopTenHighestEarningNames();

        assertThat(topTen).hasSize(5);
    }

    @Test
    void getTopTenHighestEarningNames_shouldReturnEmptyList_whenNoEmployees() {
        employeeService.setEmployees(List.of());

        List<String> topTen = employeeService.getTopTenHighestEarningNames();

        assertThat(topTen).isEmpty();
    }

    @Test
    void getTopTenHighestEarningNames_shouldFilterOutNullSalaries() {
        List<Employee> employeesWithNull = List.of(
                new Employee(UUID.randomUUID(), "NoSalary", null, 30, "Engineer", "no@test.com"),
                new Employee(UUID.randomUUID(), "HasSalary", 50000, 25, "Analyst", "has@test.com"));
        employeeService.setEmployees(employeesWithNull);

        List<String> topTen = employeeService.getTopTenHighestEarningNames();

        assertThat(topTen).containsExactly("HasSalary");
    }

    @Test
    void getTopTenHighestEarningNames_shouldReturnOnlyNames() {
        List<String> topTen = employeeService.getTopTenHighestEarningNames();

        // Verify we get strings, not employee objects
        assertThat(topTen).allMatch(name -> name instanceof String);
        assertThat(topTen).allMatch(name -> !name.contains("@")); // No emails
    }
}
