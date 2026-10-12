package com.energy.detection.controller;

import com.energy.detection.dto.InspectionCreateRequest;
import com.energy.detection.model.InspectionRecord;
import com.energy.detection.repository.InspectionRecordRepository;
import com.energy.detection.repository.MeterRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/energy/meters/{meterId}/inspections")
@RequiredArgsConstructor
public class InspectionController {
    private final InspectionRecordRepository inspections;
    private final MeterRepository meters;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long meterId,
                                    @Valid @RequestBody InspectionCreateRequest request,
                                    Authentication authentication) {
        var meter = meters.findById(meterId);
        if (meter.isEmpty()) return ResponseEntity.notFound().build();
        var saved = inspections.save(InspectionRecord.builder()
            .meter(meter.get())
            .createdAt(Instant.now())
            .reportedBy(authentication.getName())
            .estimatedUnaccountedKwh(request.estimatedUnaccountedKwh())
            .inspectionNotes(request.inspectionNotes())
            .status(InspectionRecord.Status.PENDING_REVIEW)
            .build());
        return ResponseEntity.created(URI.create("/api/energy/meters/" + meterId + "/inspections/" + saved.getId()))
            .body(view(saved));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long meterId) {
        if (!meters.existsById(meterId)) return ResponseEntity.notFound().build();
        List<Map<String, Object>> data = inspections.findTop100ByMeterIdOrderByCreatedAtDesc(meterId)
            .stream().map(this::view).toList();
        return ResponseEntity.ok(data);
    }

    private Map<String, Object> view(InspectionRecord record) {
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("id", record.getId());
        data.put("meterId", record.getMeter().getId());
        data.put("createdAt", record.getCreatedAt());
        data.put("reportedBy", record.getReportedBy());
        data.put("estimatedUnaccountedKwh", record.getEstimatedUnaccountedKwh());
        data.put("inspectionNotes", record.getInspectionNotes());
        data.put("status", record.getStatus().name());
        data.put("fineAmount", null);
        return data;
    }
}
