package com.familyshield.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ChildRequest(
    @NotBlank String name,
    @Min(1) int age
) {}
