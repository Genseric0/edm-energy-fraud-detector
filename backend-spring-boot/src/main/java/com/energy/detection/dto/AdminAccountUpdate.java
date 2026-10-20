package com.energy.detection.dto;

import com.energy.detection.model.AccountRole;
import jakarta.validation.constraints.*;

public record AdminAccountUpdate(@NotNull AccountRole role, @NotNull Boolean active,
                                @NotNull @PositiveOrZero Long expectedVersion,
                                @NotBlank @Size(min = 10, max = 500) String reason,
                                @NotBlank @Size(max = 72) String currentPassword) {
    @Override public String toString() { return "AdminAccountUpdate[REDACTED]"; }
}
