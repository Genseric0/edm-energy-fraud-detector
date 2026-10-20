package com.energy.detection.dto;

import com.energy.detection.model.MeterReading;
import java.math.BigDecimal;
import java.time.Instant;

public record MeterReadingView(Long id, Instant recordedAt, BigDecimal energyKwh, String source, String ingestedBy) {
    public static MeterReadingView from(MeterReading reading) {
        return new MeterReadingView(reading.getId(), reading.getRecordedAt(), reading.getEnergyKwh(), reading.getSource(), reading.getIngestedBy());
    }
}
