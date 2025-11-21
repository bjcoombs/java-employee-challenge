package com.reliaquest.api.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateEmployeeRequest(
        @NotBlank String name,
        @NotNull @Positive Integer salary,
        @NotNull @Min(16) @Max(75) Integer age,
        @NotBlank String title) {}
