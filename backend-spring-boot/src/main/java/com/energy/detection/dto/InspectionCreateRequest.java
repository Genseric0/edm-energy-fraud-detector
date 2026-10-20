package com.energy.detection.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record InspectionCreateRequest(
    @NotNull @DecimalMin("0") @DecimalMax("99999999999.999") @Digits(integer = 11, fraction = 3) BigDecimal estimatedUnaccountedKwh,
    @Size(max = 4000) String inspectionNotes
) {}
