package com.energy.detection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resumo consolidado de todas as medições para alimentação dos cards do dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private Double totalSuppliedKwh;
    private Double totalBilledKwh;
    private Double totalLossKwh;
    private Double overallLossPercentage;
    
    private Integer totalTransformers;
    private Integer criticalAlertsCount;
    private Integer normalTransformersCount;
    private Integer totalMetersCount;
    
    private Double estimatedTotalLossMt; // Perda financeira estimada total em Meticais (MT)
}
