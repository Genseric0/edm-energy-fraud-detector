package com.energy.detection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record InspectionCreateRequest(
    @NotNull @PositiveOrZero Double estimatedUnaccountedKwh,
    @Size(max = 4000) String inspectionNotes
) {}
