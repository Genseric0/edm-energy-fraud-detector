package com.energy.detection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa os dados de um medidor residencial com o
 * CÁLCULO INDIVIDUALIZADO DE PERDA atribuído a ele no setor elétrico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterDetailDTO {

    private Long id;
    private String meterNumber;
    private String residentName;
    private String phoneNumber;
    private String address;
    private Double billedKwh;

    // Cálculo Individualizado da Perda
    /** Participação percentual deste medidor no faturamento total do transformador (%) */
    private Double shareOfTotalBilledPercent;

    /** Quota individual de perda pro-rata alocada a este ramal de consumo (kWh) */
    private Double individualLossKwh;

    /** Prejuízo financeiro estimado referente à perda individual alocada em Meticais (MT / MZN) */
    private Double individualFinancialLossMt;

    /** Desvio do consumo deste medidor em relação à média do PT residencial (kWh) */
    private Double deviationFromAverageKwh;

    /**
     * Indicador de suspeita individual de fraude / bypass:
     * - "SUSPEITO_BYPASS": Setor crítico com perda > 5% E consumo deste medidor anormalmente baixo (< 50% da média)
     * - "CONSUMO_ELEVADO": Consumo significativamente superior à média (> 180%)
     * - "REGULAR": Consumo condizente com a curva de carga da região
     */
    private String suspicionStatus;
    private String suspicionDescription;
}
