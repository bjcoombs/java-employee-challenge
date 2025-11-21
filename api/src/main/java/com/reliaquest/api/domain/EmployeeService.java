package com.reliaquest.api.domain;

import com.reliaquest.api.domain.port.EmployeePort;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final EmployeePort employeePort;

    public EmployeeService(EmployeePort employeePort) {
        this.employeePort = employeePort;
    }

    public List<Employee> getAllEmployees() {
        return employeePort.findAll();
    }

    public List<Employee> searchByName(String searchString) {
        return employeePort.findAll().stream()
                .filter(e -> e.name().toLowerCase().contains(searchString.toLowerCase()))
                .toList();
    }

    public Employee getById(UUID id) {
        return employeePort.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    public Integer getHighestSalary() {
        return employeePort.findAll().stream().mapToInt(Employee::salary).max().orElse(0);
    }

    public List<String> getTopTenHighestEarningNames() {
        return employeePort.findAll().stream()
                .sorted(Comparator.comparingInt(Employee::salary).reversed())
                .limit(10)
                .map(Employee::name)
                .toList();
    }

    public Employee create(CreateEmployeeRequest request) {
        return employeePort.create(request);
    }

    public String deleteById(UUID id) {
        Employee employee = getById(id);
        employeePort.deleteByName(employee.name());
        return employee.name();
    }
}
