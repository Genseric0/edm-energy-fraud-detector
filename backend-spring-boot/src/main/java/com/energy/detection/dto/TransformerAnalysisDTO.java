package com.energy.detection.dto;

import com.energy.detection.model.SectorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que consolida a análise de balanço energético de um transformador,
 * aplicando a regra de detecção de perdas não técnicas (ligações clandestinas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformerAnalysisDTO {
    private Long transformerId;
    private String transformerCode;
    private String neighborhood;
    private Double capacityKva;
    
    // Dados energéticos (kWh)
    private Double totalSuppliedKwh;  // Energia Fornecida pelo Transformador
    private Double totalBilledKwh;    // Soma da Energia dos Medidores Faturados
    private Double lossKwh;           // Perda Total (kWh) = Fornecida - Faturada
    private Double lossPercentage;    // Percentual de Perda = (Perda / Fornecida) * 100
    
    // Análise de Irregularidade
    private Double technicalLossThreshold; // Limite de perda técnica (5.0%)
    private SectorStatus status;           // NORMAL ou CRITICAL_ALERT
    private String statusDescription;      // "Normal: Perda Técnica Aceitável" ou "Alerta Crítico: Suspeita de Ligação Clandestina"
    private Boolean isCriticalLoss;        // true se perda > 5.0%
    
    // Métricas operacionais e financeiras (Moeda: Metical Moçambicano - MT/MZN)
    private Integer metersCount;
    private Double estimatedFinancialLossMt;
}
