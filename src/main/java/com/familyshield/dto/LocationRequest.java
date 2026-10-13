package com.familyshield.dto;

import jakarta.validation.constraints.NotNull;

public record LocationRequest(
    @NotNull Double latitude,
    @NotNull Double longitude
) {}
