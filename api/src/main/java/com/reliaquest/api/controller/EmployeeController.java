package com.reliaquest.api.controller;

import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import com.reliaquest.api.domain.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeRequest> {

    private static final Logger logger = LogManager.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("Processing request: GET /api/v1/employee");
        try {
            return ResponseEntity.ok(employeeService.getAllEmployees());
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(@PathVariable String searchString) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("Processing request: GET /api/v1/employee/search/{}", searchString);
        try {
            return ResponseEntity.ok(employeeService.searchByName(searchString));
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String id) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("Processing request: GET /api/v1/employee/{}", id);
        try {
            UUID uuid = UUID.fromString(id);
            return ResponseEntity.ok(employeeService.getById(uuid));
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("Processing request: GET /api/v1/employee/highestSalary");
        try {
            return ResponseEntity.ok(employeeService.getHighestSalary());
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("Processing request: GET /api/v1/employee/topTenHighestEarningEmployeeNames");
        try {
            return ResponseEntity.ok(employeeService.getTopTenHighestEarningNames());
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody CreateEmployeeRequest employeeInput) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("Processing request: POST /api/v1/employee");
        try {
            return ResponseEntity.ok(employeeService.create(employeeInput));
        } finally {
            MDC.remove("correlationId");
        }
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(@PathVariable String id) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("Processing request: DELETE /api/v1/employee/{}", id);
        try {
            UUID uuid = UUID.fromString(id);
            String deletedName = employeeService.deleteById(uuid);
            return ResponseEntity.ok(deletedName);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
