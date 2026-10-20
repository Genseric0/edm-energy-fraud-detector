package com.energy.detection.dto;

import jakarta.validation.constraints.*;

public record AdminAccountAction(@NotNull @PositiveOrZero Long expectedVersion,
                                @NotBlank @Size(min = 10, max = 500) String reason,
                                @NotBlank @Size(max = 72) String currentPassword) {
    @Override public String toString() { return "AdminAccountAction[REDACTED]"; }
}
