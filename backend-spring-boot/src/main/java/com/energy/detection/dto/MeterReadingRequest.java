package com.energy.detection.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.math.BigDecimal;

public record MeterReadingRequest(
    @NotNull Instant recordedAt,
    @NotNull @DecimalMin("0") @DecimalMax("99999999999.999") @Digits(integer = 11, fraction = 3) BigDecimal energyKwh
) {
    @AssertTrue
    public boolean isTimestampPrecisionSupported() {
        return recordedAt == null || recordedAt.getNano() % 1000 == 0;
    }
}
