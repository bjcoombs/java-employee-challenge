package com.reliaquest.api.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.reliaquest.api.domain.port.EmployeePort;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class EmployeeServiceCacheTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private EmployeePort employeePort;

    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(cacheManager.getCache("employees")).clear();
        testEmployee = new Employee(
                UUID.randomUUID(), "John Doe", 50000, 30, "Developer", "john@company.com");
    }

    @Test
    @DisplayName("getAllEmployees should cache results on first call")
    void getAllEmployees_shouldCacheResults() {
        when(employeePort.findAll()).thenReturn(List.of(testEmployee));

        List<Employee> firstCall = employeeService.getAllEmployees();
        List<Employee> secondCall = employeeService.getAllEmployees();

        assertThat(firstCall).hasSize(1);
        assertThat(secondCall).hasSize(1);
        verify(employeePort, times(1)).findAll();
    }

    @Test
    @DisplayName("create should evict cache")
    void create_shouldEvictCache() {
        when(employeePort.findAll()).thenReturn(List.of(testEmployee));
        CreateEmployeeRequest request = new CreateEmployeeRequest("Jane Doe", 60000, 25, "Manager");
        Employee createdEmployee = new Employee(
                UUID.randomUUID(), "Jane Doe", 60000, 25, "Manager", "jane@company.com");
        when(employeePort.create(request)).thenReturn(createdEmployee);

        employeeService.getAllEmployees();
        employeeService.create(request);
        employeeService.getAllEmployees();

        verify(employeePort, times(2)).findAll();
    }

    @Test
    @DisplayName("deleteById should evict cache")
    void deleteById_shouldEvictCache() {
        UUID id = testEmployee.id();
        when(employeePort.findAll()).thenReturn(List.of(testEmployee));
        when(employeePort.findById(id)).thenReturn(java.util.Optional.of(testEmployee));
        when(employeePort.deleteByName(testEmployee.name())).thenReturn(true);

        employeeService.getAllEmployees();
        employeeService.deleteById(id);
        employeeService.getAllEmployees();

        verify(employeePort, times(2)).findAll();
    }

    @Test
    @DisplayName("cache should return same data on subsequent calls")
    void cache_shouldReturnSameData() {
        when(employeePort.findAll()).thenReturn(List.of(testEmployee));

        List<Employee> firstCall = employeeService.getAllEmployees();

        when(employeePort.findAll()).thenReturn(List.of()); // Change mock return

        List<Employee> secondCall = employeeService.getAllEmployees();

        assertThat(firstCall).isEqualTo(secondCall);
        assertThat(secondCall).hasSize(1);
    }

    @Test
    @DisplayName("searchByName should return filtered results")
    void searchByName_shouldReturnFilteredResults() {
        when(employeePort.findAll()).thenReturn(List.of(testEmployee));

        List<Employee> results = employeeService.searchByName("John");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("getHighestSalary should return correct value")
    void getHighestSalary_shouldReturnCorrectValue() {
        when(employeePort.findAll()).thenReturn(List.of(testEmployee));

        int salary = employeeService.getHighestSalary();

        assertThat(salary).isEqualTo(50000);
    }

    @Test
    @DisplayName("getTopTenHighestEarningNames should return names in order")
    void getTopTenHighestEarningNames_shouldReturnNamesInOrder() {
        when(employeePort.findAll()).thenReturn(List.of(testEmployee));

        List<String> names = employeeService.getTopTenHighestEarningNames();

        assertThat(names).containsExactly("John Doe");
    }
}
