import { apiClient } from './client';
import { DashboardSummary, TransformerAnalysis, TransformerDetailResponse, Meter } from '../types/energy';

/**
 * Camada de acesso aos endpoints de /api/energy do backend Spring Boot.
 * Cada função corresponde diretamente a um método de EnergyController.java.
 */
export const energyApi = {
  getSummary: () => apiClient.get<DashboardSummary>('/api/energy/summary'),

  getAllTransformers: () => apiClient.get<TransformerAnalysis[]>('/api/energy/transformers'),

  getCriticalAlerts: () => apiClient.get<TransformerAnalysis[]>('/api/energy/alerts'),

  getTransformerDetails: (id: number) =>
    apiClient.get<TransformerDetailResponse>(`/api/energy/transformers/${id}/details`),

  getTransformerById: (id: number) =>
    apiClient.get<TransformerAnalysis>(`/api/energy/transformers/${id}`),

  getMetersByTransformer: (id: number) =>
    apiClient.get<Meter[]>(`/api/energy/transformers/${id}/meters`),

  simulateTheft: (id: number, clandestineKwh?: number) =>
    apiClient.post<TransformerAnalysis>(`/api/energy/transformers/${id}/simulate-theft`, {
      clandestineKwh: clandestineKwh ?? 2400,
    }),

  normalizeSector: (id: number) =>
    apiClient.post<TransformerAnalysis>(`/api/energy/transformers/${id}/normalize`),
};
