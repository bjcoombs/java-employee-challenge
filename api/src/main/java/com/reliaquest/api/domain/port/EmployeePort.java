package com.reliaquest.api.domain.port;

import com.reliaquest.api.domain.CreateEmployeeRequest;
import com.reliaquest.api.domain.Employee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeePort {

    List<Employee> findAll();

    Optional<Employee> findById(UUID id);

    Employee create(CreateEmployeeRequest request);

    boolean deleteByName(String name);
}
