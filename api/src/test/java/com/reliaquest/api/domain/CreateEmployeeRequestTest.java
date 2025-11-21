package com.reliaquest.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateEmployeeRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldPassValidationWithValidInput() {
        var request = new CreateEmployeeRequest("John Doe", 50000, 30, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidationWithBlankName() {
        var request = new CreateEmployeeRequest("", 50000, 30, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    void shouldFailValidationWithNullName() {
        var request = new CreateEmployeeRequest(null, 50000, 30, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    void shouldFailValidationWithNegativeSalary() {
        var request = new CreateEmployeeRequest("John Doe", -1000, 30, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("salary");
    }

    @Test
    void shouldFailValidationWithZeroSalary() {
        var request = new CreateEmployeeRequest("John Doe", 0, 30, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("salary");
    }

    @Test
    void shouldFailValidationWithNullSalary() {
        var request = new CreateEmployeeRequest("John Doe", null, 30, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("salary");
    }

    @Test
    void shouldFailValidationWithAgeBelowMinimum() {
        var request = new CreateEmployeeRequest("John Doe", 50000, 15, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("age");
    }

    @Test
    void shouldFailValidationWithAgeAboveMaximum() {
        var request = new CreateEmployeeRequest("John Doe", 50000, 76, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("age");
    }

    @Test
    void shouldPassValidationWithMinimumAge() {
        var request = new CreateEmployeeRequest("John Doe", 50000, 16, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldPassValidationWithMaximumAge() {
        var request = new CreateEmployeeRequest("John Doe", 50000, 75, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidationWithNullAge() {
        var request = new CreateEmployeeRequest("John Doe", 50000, null, "Developer");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("age");
    }

    @Test
    void shouldFailValidationWithBlankTitle() {
        var request = new CreateEmployeeRequest("John Doe", 50000, 30, "");

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("title");
    }
}
