/**
 * Constantes de negócio do Sistema de Detecção de Perdas Não Técnicas (EDM).
 * Mantidas em sincronia manual com EnergyAnalysisService.java no backend
 * (TECHNICAL_LOSS_THRESHOLD_PERCENT e AVERAGE_TARIFF_PER_KWH_MT).
 */

/** Limiar demonstrativo (%), não uma norma oficial validada. */
export const TECHNICAL_LOSS_THRESHOLD = 5.0;

/** Valor demonstrativo (MT/kWh), não tarifa oficial nem base para cobrança. */
export const AVERAGE_TARIFF_MZN = 7.5;
