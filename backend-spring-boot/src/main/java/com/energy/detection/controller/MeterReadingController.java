package com.energy.detection.controller;

import com.energy.detection.dto.MeterReadingRequest;
import com.energy.detection.model.MeterReading;
import com.energy.detection.repository.MeterReadingRepository;
import com.energy.detection.repository.MeterRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** API inicial de recolha manual; integração AMI real exigirá autenticação de dispositivos e validação adicional. */
@RestController
@RequestMapping("/api/energy/meters/{meterId}/readings")
@RequiredArgsConstructor
public class MeterReadingController {
    private final MeterReadingRepository readings;
    private final MeterRepository meters;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long meterId) {
        if (!meters.existsById(meterId)) return ResponseEntity.notFound().build();
        List<Map<String, Object>> history = readings.findTop100ByMeterIdOrderByRecordedAtDesc(meterId)
            .stream().map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "recordedAt", r.getRecordedAt(),
                "energyKwh", r.getEnergyKwh(),
                "source", r.getSource()
            )).toList();
        return ResponseEntity.ok(history);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long meterId, @Valid @RequestBody MeterReadingRequest input) {
        var meter = meters.findById(meterId);
        if (meter.isEmpty()) return ResponseEntity.notFound().build();
        if (input.recordedAt().isAfter(java.time.Instant.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data de leitura não pode estar no futuro"));
        }
        MeterReading reading = readings.save(MeterReading.builder()
            .meter(meter.get())
            .recordedAt(input.recordedAt())
            .energyKwh(input.energyKwh())
            .source("MANUAL")
            .build());
        return ResponseEntity.created(URI.create("/api/energy/meters/" + meterId + "/readings/" + reading.getId()))
            .body(Map.of("id", reading.getId(), "recordedAt", reading.getRecordedAt(),
                "energyKwh", reading.getEnergyKwh(), "source", reading.getSource()));
    }
}
