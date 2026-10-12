package com.energy.detection.controller;

import com.energy.detection.dto.TransformerAnalysisDTO;
import com.energy.detection.service.EnergyAnalysisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Mutações sintéticas: estas rotas não existem fora do perfil demo. */
@RestController
@Profile("demo")
@RequestMapping("/api/energy/transformers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DemoEnergyController {
    private final EnergyAnalysisService energyAnalysisService;

    public record SimulationRequest(
        @NotNull @DecimalMin("0.001") @DecimalMax("1000000") Double clandestineKwh
    ) {}

    @PostMapping("/{id}/simulate-theft")
    public ResponseEntity<TransformerAnalysisDTO> simulate(
            @PathVariable Long id, @Valid @RequestBody SimulationRequest request) {
        return ResponseEntity.ok(energyAnalysisService.simulateTheft(id, request.clandestineKwh()));
    }

    @PostMapping("/{id}/normalize")
    public ResponseEntity<TransformerAnalysisDTO> normalize(@PathVariable Long id) {
        return ResponseEntity.ok(energyAnalysisService.normalizeSector(id));
    }
}
