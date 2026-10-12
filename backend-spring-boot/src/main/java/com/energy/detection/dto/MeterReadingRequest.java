package com.energy.detection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

public record MeterReadingRequest(
    @NotNull Instant recordedAt,
    @NotNull @PositiveOrZero Double energyKwh
) {}
