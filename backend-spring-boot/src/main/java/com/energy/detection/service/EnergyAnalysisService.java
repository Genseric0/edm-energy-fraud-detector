package com.energy.detection.service;

import com.energy.detection.dto.DashboardSummaryDTO;
import com.energy.detection.dto.MeterDTO;
import com.energy.detection.dto.MeterDetailDTO;
import com.energy.detection.dto.TransformerAnalysisDTO;
import com.energy.detection.dto.TransformerDetailResponseDTO;
import com.energy.detection.model.Meter;
import com.energy.detection.model.SectorStatus;
import com.energy.detection.model.Transformer;
import com.energy.detection.repository.MeterRepository;
import com.energy.detection.repository.TransformerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo cálculo e auditoria de balanço energético e
 * detecção de perdas não técnicas (fraudes e ligações clandestinas).
 */
@Service
@RequiredArgsConstructor
public class EnergyAnalysisService {

    private final TransformerRepository transformerRepository;
    private final MeterRepository meterRepository;

    /**
     * Margem de tolerância para perdas técnicas inerentes à resistência de condutores
     * e efeito Joule (5.0%) como parâmetro demonstrativo, não como norma oficial validada.
     * Acima do limiar há discrepância a investigar, não prova de fraude.
     */
    public static final double TECHNICAL_LOSS_THRESHOLD_PERCENT = 5.0;

    /**
     * Tarifa hipotética de demonstração para cálculo de prejuízo financeiro (7,50 MT por kWh).
     * Moeda oficial: Metical Moçambicano (MZN / MT).
     */
    public static final double AVERAGE_TARIFF_PER_KWH_MT = 7.50;

    /**
     * Calcula a análise energética completa de um transformador (PT) aplicando a regra de negócio:
     * Perda = (Energia do Transformador) - (Soma dos Medidores)
     * % Perda = (Perda / Energia do Transformador) * 100
     * Se % Perda > 5% -> Alerta Crítico: Suspeita de Ligação Clandestina
     */
    public TransformerAnalysisDTO calculateAnalysis(Transformer transformer) {
        double totalSupplied = transformer.getTotalSuppliedKwh() != null ? transformer.getTotalSuppliedKwh() : 0.0;

        // Soma da energia consumida (e faturada) pelos medidores conectados
        double totalBilled = 0.0;
        int metersCount = 0;

        if (transformer.getMeters() != null) {
            metersCount = transformer.getMeters().size();
            totalBilled = transformer.getMeters().stream()
                    .mapToDouble(meter -> meter.getBilledKwh() != null ? meter.getBilledKwh() : 0.0)
                    .sum();
        }

        // Perda = Fornecido - Faturado
        double lossKwh = Math.max(0.0, totalSupplied - totalBilled);
        
        // Percentual de Perda
        double lossPercentage = (totalSupplied > 0.0) ? (lossKwh / totalSupplied) * 100.0 : 0.0;

        // Regra de Negócio EDM: Se a diferença for maior que 5%, classifica como Alerta Crítico
        boolean isCriticalLoss = lossPercentage > TECHNICAL_LOSS_THRESHOLD_PERCENT;
        SectorStatus status = isCriticalLoss ? SectorStatus.CRITICAL_ALERT : SectorStatus.NORMAL;

        // Estimativa de prejuízo financeiro referente à parcela não técnica (acima dos 5%) em Meticais (MT)
        double nonTechnicalLossKwh = Math.max(0.0, lossKwh - (totalSupplied * (TECHNICAL_LOSS_THRESHOLD_PERCENT / 100.0)));
        double estimatedFinancialLoss = round(nonTechnicalLossKwh * AVERAGE_TARIFF_PER_KWH_MT);

        return TransformerAnalysisDTO.builder()
                .transformerId(transformer.getId())
                .transformerCode(transformer.getCode())
                .neighborhood(transformer.getNeighborhood())
                .capacityKva(transformer.getCapacityKva())
                .totalSuppliedKwh(round(totalSupplied))
                .totalBilledKwh(round(totalBilled))
                .lossKwh(round(lossKwh))
                .lossPercentage(round(lossPercentage))
                .technicalLossThreshold(TECHNICAL_LOSS_THRESHOLD_PERCENT)
                .status(status)
                .statusDescription(status.getDescription())
                .isCriticalLoss(isCriticalLoss)
                .metersCount(metersCount)
                .estimatedFinancialLossMt(estimatedFinancialLoss)
                .build();
    }

    /**
     * Retorna a análise de todos os transformadores cadastrados.
     */
    @Transactional(readOnly = true)
    public List<TransformerAnalysisDTO> getAllAnalyses() {
        return transformerRepository.findAllWithMeters().stream()
                .map(this::calculateAnalysis)
                .collect(Collectors.toList());
    }

    /**
     * Retorna apenas os transformadores com alerta crítico (suspeita de ligação clandestina).
     */
    @Transactional(readOnly = true)
    public List<TransformerAnalysisDTO> getCriticalAlerts() {
        return getAllAnalyses().stream()
                .filter(TransformerAnalysisDTO::getIsCriticalLoss)
                .collect(Collectors.toList());
    }

    /**
     * Retorna a análise detalhada de um transformador específico pelo ID.
     */
    @Transactional(readOnly = true)
    public TransformerAnalysisDTO getAnalysisById(Long transformerId) {
        Transformer transformer = transformerRepository.findByIdWithMeters(transformerId)
                .orElseThrow(() -> new RuntimeException("Transformador não encontrado com o ID: " + transformerId));
        return calculateAnalysis(transformer);
    }

    /**
     * Retorna os detalhes de um transformador específico, incluindo a lista de todos
     * os medidores residenciais conectados a ele e o CÁLCULO INDIVIDUALIZADO DA PERDA para cada medidor.
     */
    @Transactional(readOnly = true)
    public TransformerDetailResponseDTO getTransformerDetailsWithIndividualMeters(Long transformerId) {
        Transformer transformer = transformerRepository.findByIdWithMeters(transformerId)
                .orElseThrow(() -> new RuntimeException("Transformador não encontrado com o ID: " + transformerId));

        TransformerAnalysisDTO summary = calculateAnalysis(transformer);
        List<Meter> meters = transformer.getMeters() != null ? transformer.getMeters() : List.of();

        double totalBilled = summary.getTotalBilledKwh();
        double totalLoss = summary.getLossKwh();
        int count = meters.size();
        double averageConsumption = count > 0 ? (totalBilled / count) : 0.0;

        List<MeterDetailDTO> detailedMeters = meters.stream().map(m -> {
            double billed = m.getBilledKwh() != null ? m.getBilledKwh() : 0.0;

            // 1. Participação percentual no faturamento total do transformador (%)
            double sharePercent = totalBilled > 0 ? (billed / totalBilled) * 100.0 : 0.0;

            // 2. Cálculo individualizado da perda pro-rata alocada a este ramal residencial (kWh)
            double individualLoss = totalBilled > 0 
                    ? (totalLoss * (billed / totalBilled)) 
                    : (count > 0 ? totalLoss / count : 0.0);

            // 3. Prejuízo financeiro individualizado (MT)
            double financialLoss = individualLoss * AVERAGE_TARIFF_PER_KWH_MT;

            // 4. Desvio em relação à média do setor residencial (kWh)
            double deviation = billed - averageConsumption;

            // 5. Diagnóstico individual de suspeita de fraude / bypass
            String status = "REGULAR";
            String description = "Consumo regular e compatível com a curva de carga da região.";

            if (summary.getIsCriticalLoss() && billed < (averageConsumption * 0.5)) {
                status = "SUSPEITO_BYPASS";
                description = "Consumo abaixo da média num sector com discrepância. Necessária verificação técnica; não comprova bypass.";
            } else if (billed > (averageConsumption * 1.8)) {
                status = "CONSUMO_ELEVADO";
                description = "Consumo substancialmente superior à média residencial da região.";
            }

            return MeterDetailDTO.builder()
                    .id(m.getId())
                    .meterNumber(m.getMeterNumber())
                    .residentName(m.getResidentName())
                    .phoneNumber(m.getPhoneNumber())
                    .address(m.getAddress())
                    .billedKwh(round(billed))
                    .installationLocation(m.getInstallationLocation().name())
                    .remoteReadingEnabled(m.isRemoteReadingEnabled())
                    .shareOfTotalBilledPercent(round(sharePercent))
                    .individualLossKwh(round(individualLoss))
                    .individualFinancialLossMt(round(financialLoss))
                    .deviationFromAverageKwh(round(deviation))
                    .suspicionStatus(status)
                    .suspicionDescription(description)
                    .build();
        }).collect(Collectors.toList());

        return TransformerDetailResponseDTO.builder()
                .transformerId(transformer.getId())
                .transformerCode(transformer.getCode())
                .neighborhood(transformer.getNeighborhood())
                .capacityKva(transformer.getCapacityKva())
                .totalSuppliedKwh(summary.getTotalSuppliedKwh())
                .totalBilledKwh(summary.getTotalBilledKwh())
                .lossKwh(summary.getLossKwh())
                .lossPercentage(summary.getLossPercentage())
                .technicalLossThreshold(TECHNICAL_LOSS_THRESHOLD_PERCENT)
                .status(summary.getStatus())
                .statusDescription(summary.getStatusDescription())
                .isCriticalLoss(summary.getIsCriticalLoss())
                .metersCount(count)
                .averageMeterConsumptionKwh(round(averageConsumption))
                .estimatedFinancialLossMt(summary.getEstimatedFinancialLossMt())
                .meters(detailedMeters)
                .build();
    }

    /**
     * Retorna os medidores conectados a um transformador específico.
     */
    @Transactional(readOnly = true)
    public List<MeterDTO> getMetersByTransformerId(Long transformerId) {
        Transformer transformer = transformerRepository.findByIdWithMeters(transformerId)
                .orElseThrow(() -> new RuntimeException("Transformador não encontrado com o ID: " + transformerId));

        return transformer.getMeters().stream()
                .map(m -> MeterDTO.builder()
                        .id(m.getId())
                        .meterNumber(m.getMeterNumber())
                        .residentName(m.getResidentName())
                        .phoneNumber(m.getPhoneNumber())
                        .address(m.getAddress())
                        .billedKwh(round(m.getBilledKwh()))
                        .installationLocation(m.getInstallationLocation().name())
                        .remoteReadingEnabled(m.isRemoteReadingEnabled())
                        .transformerId(transformer.getId())
                        .transformerCode(transformer.getCode())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Gera o sumário executivo para os cards de topo do dashboard.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary() {
        List<TransformerAnalysisDTO> analyses = getAllAnalyses();

        double totalSupplied = analyses.stream().mapToDouble(TransformerAnalysisDTO::getTotalSuppliedKwh).sum();
        double totalBilled = analyses.stream().mapToDouble(TransformerAnalysisDTO::getTotalBilledKwh).sum();
        double totalLoss = Math.max(0.0, totalSupplied - totalBilled);
        double overallLossPercentage = totalSupplied > 0 ? (totalLoss / totalSupplied) * 100.0 : 0.0;

        int criticalAlerts = (int) analyses.stream().filter(TransformerAnalysisDTO::getIsCriticalLoss).count();
        int normalTransformers = analyses.size() - criticalAlerts;
        int totalMeters = analyses.stream().mapToInt(TransformerAnalysisDTO::getMetersCount).sum();

        double totalLossMt = analyses.stream()
                .mapToDouble(a -> a.getEstimatedFinancialLossMt() != null ? a.getEstimatedFinancialLossMt() : 0.0)
                .sum();

        return DashboardSummaryDTO.builder()
                .totalSuppliedKwh(round(totalSupplied))
                .totalBilledKwh(round(totalBilled))
                .totalLossKwh(round(totalLoss))
                .overallLossPercentage(round(overallLossPercentage))
                .totalTransformers(analyses.size())
                .criticalAlertsCount(criticalAlerts)
                .normalTransformersCount(normalTransformers)
                .totalMetersCount(totalMeters)
                .estimatedTotalLossMt(round(totalLossMt))
                .build();
    }

    /**
     * Simula a introdução de uma ligação clandestina (aumenta a energia fornecida pelo
     * transformador sem registro nos medidores legais, simulando consumo roubado).
     */
    @Transactional
    public TransformerAnalysisDTO simulateTheft(Long transformerId, Double clandestineLoadKwh) {
        Transformer transformer = transformerRepository.findByIdWithMeters(transformerId)
                .orElseThrow(() -> new RuntimeException("Transformador não encontrado com o ID: " + transformerId));

        double additional = (clandestineLoadKwh != null && clandestineLoadKwh > 0) ? clandestineLoadKwh : 1800.0;
        transformer.setTotalSuppliedKwh(transformer.getTotalSuppliedKwh() + additional);
        transformerRepository.save(transformer);

        return calculateAnalysis(transformer);
    }

    /**
     * Normaliza as medições de um setor simulando regularização da rede (reduz perda para patamar técnico <= 5%).
     */
    @Transactional
    public TransformerAnalysisDTO normalizeSector(Long transformerId) {
        Transformer transformer = transformerRepository.findByIdWithMeters(transformerId)
                .orElseThrow(() -> new RuntimeException("Transformador não encontrado com o ID: " + transformerId));

        double totalBilled = transformer.getMeters().stream()
                .mapToDouble(Meter::getBilledKwh)
                .sum();

        // Ajusta o fornecimento para uma perda técnica realista de 3.5% (dentro do limite aceitável de 5%)
        double normalizedSupply = totalBilled / (1.0 - 0.035);
        transformer.setTotalSuppliedKwh(round(normalizedSupply));
        transformerRepository.save(transformer);

        return calculateAnalysis(transformer);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
