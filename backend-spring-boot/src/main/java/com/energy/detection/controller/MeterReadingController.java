package com.energy.detection.controller;

import com.energy.detection.dto.MeterReadingRequest;
import com.energy.detection.dto.MeterReadingView;
import com.energy.detection.model.MeterReading;
import com.energy.detection.repository.MeterReadingRepository;
import com.energy.detection.repository.MeterRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
        List<MeterReadingView> history = readings.findTop100ByMeterIdOrderByRecordedAtDesc(meterId)
            .stream().map(MeterReadingView::from).toList();
        return ResponseEntity.ok(history);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long meterId, @Valid @RequestBody MeterReadingRequest input, Authentication authentication) {
        var meter = meters.findById(meterId);
        if (meter.isEmpty()) return ResponseEntity.notFound().build();
        if (input.recordedAt().isAfter(java.time.Instant.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Data de leitura não pode estar no futuro"));
        }
        MeterReading reading = readings.saveAndFlush(MeterReading.builder()
            .meter(meter.get())
            .recordedAt(input.recordedAt())
            .energyKwh(input.energyKwh())
            .source("MANUAL")
            .ingestedBy(authentication.getName())
            .build());
        return ResponseEntity.created(URI.create("/api/energy/meters/" + meterId + "/readings/" + reading.getId()))
            .body(MeterReadingView.from(reading));
    }
}
