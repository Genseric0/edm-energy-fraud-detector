export type SectorStatus = 'NORMAL' | 'CRITICAL_ALERT';
export type MeterLocation = 'POLE' | 'INSIDE_HOME' | 'OUTSIDE_HOME' | 'UNKNOWN';

export interface Meter {
  id: number;
  meterNumber: string;
  residentName: string;
  phoneNumber?: string;
  address: string;
  billedKwh: number;
  installationLocation?: MeterLocation;
  remoteReadingEnabled?: boolean;
  transformerId: number;
  transformerCode: string;
}

export interface MeterDetail {
  id: number;
  meterNumber: string;
  residentName: string;
  phoneNumber?: string;
  address: string;
  billedKwh: number;
  installationLocation?: MeterLocation;
  remoteReadingEnabled?: boolean;
  // Cálculo individualizado da perda (Moçambique - EDM)
  shareOfTotalBilledPercent: number;
  individualLossKwh: number;
  individualFinancialLossMt: number; // Prejuízo em Meticais (MT)
  deviationFromAverageKwh: number;
  suspicionStatus: 'REGULAR' | 'SUSPEITO_BYPASS' | 'CONSUMO_ELEVADO';
  suspicionDescription: string;
}

export interface TransformerDetailResponse {
  transformerId: number;
  transformerCode: string;
  neighborhood: string;
  capacityKva: number;
  totalSuppliedKwh: number;
  totalBilledKwh: number;
  lossKwh: number;
  lossPercentage: number;
  technicalLossThreshold: number;
  status: SectorStatus;
  statusDescription: string;
  isCriticalLoss: boolean;
  metersCount: number;
  averageMeterConsumptionKwh: number;
  estimatedFinancialLossMt: number; // Prejuízo em Meticais (MT)
  meters: MeterDetail[];
}

export interface AuthUser {
  username: string;
  name: string;
  role: 'ROLE_ADMIN' | 'ROLE_OPERATOR';
  authorities: string[];
}

/** Espelha LoginResponseDTO.java */
export interface LoginResponse {
  authenticated: boolean;
  token?: string;
  username?: string;
  name?: string;
  role?: 'ROLE_ADMIN' | 'ROLE_OPERATOR';
  authorities?: string[];
  message?: string;
}

export interface TransformerAnalysis {
  transformerId: number;
  transformerCode: string;
  neighborhood: string;
  capacityKva: number;
  totalSuppliedKwh: number;
  totalBilledKwh: number;
  lossKwh: number;
  lossPercentage: number;
  technicalLossThreshold: number; // 5.0%
  status: SectorStatus;
  statusDescription: string;
  isCriticalLoss: boolean; // true se perda > 5.0%
  metersCount: number;
  estimatedFinancialLossMt: number; // Prejuízo em Meticais (MT)
}

export interface DashboardSummary {
  totalSuppliedKwh: number;
  totalBilledKwh: number;
  totalLossKwh: number;
  overallLossPercentage: number;
  totalTransformers: number;
  criticalAlertsCount: number;
  normalTransformersCount: number;
  totalMetersCount: number;
  estimatedTotalLossMt: number; // Prejuízo total em Meticais (MT)
}
