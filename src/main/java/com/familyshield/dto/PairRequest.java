package com.familyshield.dto;

import jakarta.validation.constraints.NotBlank;

public record PairRequest(
    @NotBlank String pairingCode,
    @NotBlank String deviceName,
    @NotBlank String deviceUuid,
    @NotBlank String platform
) {}
