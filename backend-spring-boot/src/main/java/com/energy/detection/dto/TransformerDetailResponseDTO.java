package com.energy.detection.dto;

import com.energy.detection.model.SectorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO com os detalhes completos de um transformador específico,
 * incluindo o balanço global e a lista de medidores com seus cálculos individualizados de perda.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformerDetailResponseDTO {

    private Long transformerId;
    private String transformerCode;
    private String neighborhood;
    private Double capacityKva;

    // Métricas do Circuito
    private Double totalSuppliedKwh;
    private Double totalBilledKwh;
    private Double lossKwh;
    private Double lossPercentage;
    private Double technicalLossThreshold; // 5.0%

    // Classificação
    private SectorStatus status;
    private String statusDescription;
    private Boolean isCriticalLoss;

    // Métricas Financeiras e Estatísticas (Moeda: Metical Moçambicano - MT/MZN)
    private Integer metersCount;
    private Double averageMeterConsumptionKwh;
    private Double estimatedFinancialLossMt;

    // Lista de medidores com o cálculo individualizado da perda
    private List<MeterDetailDTO> meters;
}
