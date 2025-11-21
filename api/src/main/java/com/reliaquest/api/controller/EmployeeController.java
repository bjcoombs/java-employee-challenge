package com.reliaquest.api.controller;

import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.domain.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeRequest> {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        logger.info("Processing request: GET /api/v1/employee");
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(String searchString) {
        logger.info("Processing request: GET /api/v1/employee/search/{}", searchString);
        return ResponseEntity.ok(employeeService.searchByName(searchString));
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(String id) {
        logger.info("Processing request: GET /api/v1/employee/{}", id);
        UUID uuid = parseEmployeeId(id);
        return ResponseEntity.ok(employeeService.getById(uuid));
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        logger.info("Processing request: GET /api/v1/employee/highestSalary");
        return ResponseEntity.ok(employeeService.getHighestSalary());
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        logger.info("Processing request: GET /api/v1/employee/topTenHighestEarningEmployeeNames");
        return ResponseEntity.ok(employeeService.getTopTenHighestEarningNames());
    }

    @Override
    public ResponseEntity<Employee> createEmployee(@Valid CreateEmployeeRequest employeeInput) {
        logger.info("Processing request: POST /api/v1/employee");
        return ResponseEntity.ok(employeeService.create(employeeInput));
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        logger.info("Processing request: DELETE /api/v1/employee/{}", id);
        UUID uuid = parseEmployeeId(id);
        String deletedName = employeeService.deleteById(uuid);
        return ResponseEntity.ok(deletedName);
    }

    private UUID parseEmployeeId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid employee ID format: " + id);
        }
    }
}
