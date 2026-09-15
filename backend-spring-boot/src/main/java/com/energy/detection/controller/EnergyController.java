package com.energy.detection.controller;

import com.energy.detection.dto.DashboardSummaryDTO;
import com.energy.detection.dto.MeterDTO;
import com.energy.detection.dto.TransformerAnalysisDTO;
import com.energy.detection.dto.TransformerDetailResponseDTO;
import com.energy.detection.service.EnergyAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST com endpoints que fornecem dados consolidados e
 * análises de perdas para o Dashboard Frontend.
 */
@RestController
@RequestMapping("/api/energy")
@CrossOrigin(origins = "*") // Permite chamadas do frontend em Vite / React
@RequiredArgsConstructor
public class EnergyController {

    private final EnergyAnalysisService energyAnalysisService;

    /**
     * Retorna o resumo executivo dos dados (Total Fornecido, Faturado, Perdido, etc.)
     * GET /api/energy/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(energyAnalysisService.getDashboardSummary());
    }

    /**
     * Retorna a lista de todos os transformadores com o cálculo de balanço energético.
     * GET /api/energy/transformers
     */
    @GetMapping("/transformers")
    public ResponseEntity<List<TransformerAnalysisDTO>> getAllTransformers() {
        return ResponseEntity.ok(energyAnalysisService.getAllAnalyses());
    }

    /**
     * Retorna apenas os transformadores com "Alerta Crítico: Suspeita de Ligação Clandestina" (Perda > 5%).
     * Permissão restrita a administradores do sistema.
     * GET /api/energy/alerts
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/alerts")
    public ResponseEntity<List<TransformerAnalysisDTO>> getCriticalAlerts() {
        return ResponseEntity.ok(energyAnalysisService.getCriticalAlerts());
    }

    /**
     * Retorna os detalhes de um transformador específico, incluindo a lista de todos os
     * medidores residenciais conectados a ele e o CÁLCULO INDIVIDUALIZADO DA PERDA para cada medidor.
     * GET /api/energy/transformers/{id}/details
     */
    @GetMapping("/transformers/{id}/details")
    public ResponseEntity<TransformerDetailResponseDTO> getTransformerDetails(@PathVariable Long id) {
        return ResponseEntity.ok(energyAnalysisService.getTransformerDetailsWithIndividualMeters(id));
    }

    /**
     * Retorna os detalhes e análise de um transformador específico.
     * GET /api/energy/transformers/{id}
     */
    @GetMapping("/transformers/{id}")
    public ResponseEntity<TransformerAnalysisDTO> getTransformerById(@PathVariable Long id) {
        return ResponseEntity.ok(energyAnalysisService.getAnalysisById(id));
    }

    /**
     * Retorna todos os medidores residenciais conectados ao transformador.
     * GET /api/energy/transformers/{id}/meters
     */
    @GetMapping("/transformers/{id}/meters")
    public ResponseEntity<List<MeterDTO>> getMetersByTransformer(@PathVariable Long id) {
        return ResponseEntity.ok(energyAnalysisService.getMetersByTransformerId(id));
    }

    /**
     * Endpoint para simulação de furto / ligação clandestina (injetando carga desviada).
     * Permissão restrita a administradores do sistema.
     * POST /api/energy/transformers/{id}/simulate-theft
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/transformers/{id}/simulate-theft")
    public ResponseEntity<TransformerAnalysisDTO> simulateTheft(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Double> payload) {
        Double clandestineKwh = (payload != null && payload.containsKey("clandestineKwh")) 
                ? payload.get("clandestineKwh") : 2200.0;
        return ResponseEntity.ok(energyAnalysisService.simulateTheft(id, clandestineKwh));
    }

    /**
     * Endpoint para simulação de regularização / corte da ligação clandestina.
     * Permissão restrita a administradores do sistema.
     * POST /api/energy/transformers/{id}/normalize
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/transformers/{id}/normalize")
    public ResponseEntity<TransformerAnalysisDTO> normalizeSector(@PathVariable Long id) {
        return ResponseEntity.ok(energyAnalysisService.normalizeSector(id));
    }
}
