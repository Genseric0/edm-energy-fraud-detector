import React from 'react';
import { DashboardSummary } from '../types/energy';
import { Activity, ZapOff, DollarSign, ShieldCheck, AlertTriangle } from 'lucide-react';

interface SummaryCardsProps {
  summary: DashboardSummary;
}

export const SummaryCards: React.FC<SummaryCardsProps> = ({ summary }) => {
  const formatNumber = (num: number) => {
    return new Intl.NumberFormat('pt-MZ', { maximumFractionDigits: 0 }).format(num);
  };

  const formatCurrency = (val: number) => {
    return `${new Intl.NumberFormat('pt-MZ', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(val)} MT`;
  };

  const isGlobalCritical = summary.overallLossPercentage > 5.0;
  const estimatedLoss = summary.estimatedTotalLossMt ?? 0;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      
      {/* Card 1: Total Energia Fornecida */}
      <div className="p-4 rounded-xl bg-slate-900/90 border border-slate-800 shadow-sm relative overflow-hidden group hover:border-slate-700 transition">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Total Fornecido
          </span>
          <div className="w-8 h-8 rounded-lg bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-sky-400">
            <Activity className="w-4 h-4" />
          </div>
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-white tracking-tight">
            {formatNumber(summary.totalSuppliedKwh)}
          </span>
          <span className="text-xs text-sky-400 font-medium">kWh</span>
        </div>
        <div className="mt-2 text-xs text-slate-500 flex items-center justify-between">
          <span>{summary.totalTransformers} transformadores auditados</span>
          <span className="text-slate-400 font-mono">100% Injetado</span>
        </div>
      </div>

      {/* Card 2: Total Energia Faturada */}
      <div className="p-4 rounded-xl bg-slate-900/90 border border-slate-800 shadow-sm relative overflow-hidden group hover:border-slate-700 transition">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Total Faturado
          </span>
          <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <ShieldCheck className="w-4 h-4" />
          </div>
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-emerald-400 tracking-tight">
            {formatNumber(summary.totalBilledKwh)}
          </span>
          <span className="text-xs text-emerald-500 font-medium">kWh</span>
        </div>
        <div className="mt-2 text-xs text-slate-500 flex items-center justify-between">
          <span>{summary.totalMetersCount} medidores registrados</span>
          <span className="text-emerald-400 font-medium">
            {summary.totalSuppliedKwh > 0 
              ? `${((summary.totalBilledKwh / summary.totalSuppliedKwh) * 100).toFixed(1)}%` 
              : '0%'} Pago
          </span>
        </div>
      </div>

      {/* Card 3: Total Perdido / Desviado */}
      <div className={`p-4 rounded-xl border shadow-sm relative overflow-hidden group transition ${
        isGlobalCritical 
          ? 'bg-gradient-to-br from-rose-950/40 via-slate-900/90 to-slate-900/90 border-rose-900/50' 
          : 'bg-slate-900/90 border-slate-800'
      }`}>
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
            Total Perdido
            {isGlobalCritical && (
              <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse"></span>
            )}
          </span>
          <div className="w-8 h-8 rounded-lg bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
            <ZapOff className="w-4 h-4" />
          </div>
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-rose-400 tracking-tight">
            {formatNumber(summary.totalLossKwh)}
          </span>
          <span className="text-xs text-rose-400 font-medium">kWh</span>
        </div>
        <div className="mt-2 text-xs text-slate-500 flex items-center justify-between">
          <span className="text-rose-300/80 font-medium">
            {summary.overallLossPercentage.toFixed(1)}% perda global
          </span>
          <span className="text-xs text-slate-400">
            Limite: 5.0%
          </span>
        </div>
      </div>

      {/* Card 4: Prejuízo Financeiro & Setores em Alerta */}
      <div className="p-4 rounded-xl bg-slate-900/90 border border-slate-800 shadow-sm relative overflow-hidden group hover:border-slate-700 transition">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Prejuízo Estimado
          </span>
          <div className="w-8 h-8 rounded-lg bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
            <DollarSign className="w-4 h-4" />
          </div>
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-amber-400 tracking-tight">
            {formatCurrency(estimatedLoss)}
          </span>
        </div>
        <div className="mt-2 text-xs flex items-center justify-between">
          <span className="text-rose-400 flex items-center gap-1 font-medium">
            <AlertTriangle className="w-3 h-3" />
            {summary.criticalAlertsCount} {summary.criticalAlertsCount === 1 ? 'setor crítico' : 'setores críticos'}
          </span>
          <span className="text-emerald-400 font-medium">
            {summary.normalTransformersCount} normais
          </span>
        </div>
      </div>

    </div>
  );
};
