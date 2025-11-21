package com.reliaquest.api.domain;

import com.reliaquest.api.domain.port.EmployeePort;
import com.reliaquest.api.exception.EmployeeDeletionException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeePort employeePort;

    public EmployeeService(EmployeePort employeePort) {
        this.employeePort = employeePort;
    }

    public List<Employee> getAllEmployees() {
        logger.debug("Fetching all employees correlationId={}", MDC.get("correlationId"));
        return employeePort.findAll();
    }

    public List<Employee> searchByName(String searchString) {
        logger.debug("Searching employees by name={} correlationId={}", searchString, MDC.get("correlationId"));
        return employeePort.findAll().stream()
                .filter(e -> e.name().toLowerCase().contains(searchString.toLowerCase()))
                .toList();
    }

    public Employee getById(UUID id) {
        logger.debug("Finding employee by id={} correlationId={}", id, MDC.get("correlationId"));
        return employeePort.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Employee not found id={} correlationId={}", id, MDC.get("correlationId"));
                    return new EmployeeNotFoundException(id);
                });
    }

    public Integer getHighestSalary() {
        logger.debug("Getting highest salary correlationId={}", MDC.get("correlationId"));
        return employeePort.findAll().stream().mapToInt(Employee::salary).max().orElse(0);
    }

    public List<String> getTopTenHighestEarningNames() {
        logger.debug("Getting top ten highest earning names correlationId={}", MDC.get("correlationId"));
        return employeePort.findAll().stream()
                .sorted(Comparator.comparingInt(Employee::salary).reversed())
                .limit(10)
                .map(Employee::name)
                .toList();
    }

    public Employee create(CreateEmployeeRequest request) {
        logger.info("Creating employee name={} correlationId={}", request.name(), MDC.get("correlationId"));
        return employeePort.create(request);
    }

    public String deleteById(UUID id) {
        Employee employee = getById(id);
        logger.info("Deleting employee id={} name={} correlationId={}", id, employee.name(), MDC.get("correlationId"));
        boolean deleted = employeePort.deleteByName(employee.name());
        if (!deleted) {
            logger.error(
                    "Failed to delete employee id={} name={} correlationId={}",
                    id,
                    employee.name(),
                    MDC.get("correlationId"));
            throw new EmployeeDeletionException("Failed to delete employee: " + employee.name());
        }
        return employee.name();
    }
}
