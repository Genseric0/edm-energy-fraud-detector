/**
 * Constantes de negócio do Sistema de Detecção de Perdas Não Técnicas (EDM).
 * Mantidas em sincronia manual com EnergyAnalysisService.java no backend
 * (TECHNICAL_LOSS_THRESHOLD_PERCENT e AVERAGE_TARIFF_PER_KWH_MT).
 */

/** Margem de tolerância de perda técnica (%) segundo as normas de distribuição da EDM. */
export const TECHNICAL_LOSS_THRESHOLD = 5.0;

/** Tarifa média de referência da EDM para cálculo de prejuízo financeiro (MT por kWh). */
export const AVERAGE_TARIFF_MZN = 7.5;
