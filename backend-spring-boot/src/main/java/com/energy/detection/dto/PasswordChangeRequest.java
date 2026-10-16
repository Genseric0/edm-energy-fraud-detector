package com.energy.detection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Não incluir passwords em toString, logs ou respostas. */
public record PasswordChangeRequest(
    @NotBlank @Size(max = 72) String currentPassword,
    @NotBlank @Size(min = 12, max = 72) String newPassword
) {
    @Override public String toString() { return "PasswordChangeRequest[REDACTED]"; }
}
