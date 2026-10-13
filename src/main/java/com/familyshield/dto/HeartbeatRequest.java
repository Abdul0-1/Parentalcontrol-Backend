package com.familyshield.dto;

public record HeartbeatRequest(
    Integer batteryLevel,
    Double latitude,
    Double longitude
) {}
