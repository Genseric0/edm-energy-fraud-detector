import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { TransformerAnalysis, TransformerDetailResponse } from '../types/energy';
import { AVERAGE_TARIFF_MZN, TECHNICAL_LOSS_THRESHOLD } from '../constants/energy';
import { energyApi } from '../api/energyApi';
import { ApiError } from '../api/client';
import { generateAuditPdf } from '../utils/generateAuditPdf';
import {
  ArrowLeft,
  ShieldAlert,
  ShieldCheck,
  Zap,
  Flame,
  CheckCircle2,
  Home,
  Search,
  TrendingDown,
  AlertTriangle,
  HelpCircle,
  Gauge,
  Phone,
  FileDown,
  Loader2,
  WifiOff,
  RotateCcw
} from 'lucide-react';

interface TransformerDetailViewProps {
  transformer: TransformerAnalysis;
  onBack: () => void;
  onSimulateTheft: (id: number, additionalKwh?: number) => void;
  onNormalizeSector: (id: number) => void;
  isAdmin: boolean;
}

export const TransformerDetailView: React.FC<TransformerDetailViewProps> = ({
  transformer,
  onBack,
  onSimulateTheft,
  onNormalizeSector,
  isAdmin
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'SUSPEITO_BYPASS' | 'REGULAR' | 'CONSUMO_ELEVADO'>('ALL');
  const [sortBy, setSortBy] = useState<'loss_desc' | 'billed_asc' | 'billed_desc' | 'number'>('loss_desc');
  const [isGeneratingPdf, setIsGeneratingPdf] = useState(false);
  const [pdfMessage, setPdfMessage] = useState<string | null>(null);

  const [detailed, setDetailed] = useState<TransformerDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Busca o detalhe individualizado (perda por medidor, diagnóstico de suspeita) diretamente
  // do backend — este cálculo é feito em EnergyAnalysisService.java, não no cliente.
  const loadDetail = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await energyApi.getTransformerDetails(transformer.transformerId);
      setDetailed(data);
    } catch (err) {
      const message =
        err instanceof ApiError ? err.message : 'Erro inesperado ao carregar o detalhe do setor.';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [transformer.transformerId]);

  // Sempre que o transformador muda de estado no painel geral (ex: após simular furto
  // ou regularizar), o transformer.transformerId mantém-se igual mas os dados mudam —
  // por isso recarregamos o detalhe também quando o snapshot recebido via props muda.
  useEffect(() => {
    loadDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [transformer.totalSuppliedKwh, transformer.totalBilledKwh]);

  const filteredMeters = useMemo(() => {
    if (!detailed) return [];
    return detailed.meters
      .filter((m) => {
        const matchesSearch =
          m.meterNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
          m.residentName.toLowerCase().includes(searchTerm.toLowerCase()) ||
          m.address.toLowerCase().includes(searchTerm.toLowerCase());

        const matchesStatus =
          statusFilter === 'ALL' ? true : m.suspicionStatus === statusFilter;

        return matchesSearch && matchesStatus;
      })
      .sort((a, b) => {
        if (sortBy === 'loss_desc') return b.individualLossKwh - a.individualLossKwh;
        if (sortBy === 'billed_asc') return a.billedKwh - b.billedKwh;
        if (sortBy === 'billed_desc') return b.billedKwh - a.billedKwh;
        if (sortBy === 'number') return a.meterNumber.localeCompare(b.meterNumber);
        return 0;
      });
  }, [detailed, searchTerm, statusFilter, sortBy]);

  const handleGeneratePdf = () => {
    if (!detailed) return;
    setIsGeneratingPdf(true);
    try {
      generateAuditPdf(detailed);
      setPdfMessage(`Relatório técnico de auditoria do PT ${detailed.transformerCode} descarregado com sucesso!`);
      setTimeout(() => setPdfMessage(null), 5000);
    } catch (err) {
      console.error('Erro ao gerar relatório PDF:', err);
    } finally {
      setIsGeneratingPdf(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-24 gap-3 text-slate-400">
        <Loader2 className="w-8 h-8 text-amber-400 animate-spin" />
        <span className="text-sm">A carregar detalhe do setor...</span>
      </div>
    );
  }

  if (error || !detailed) {
    return (
      <div className="space-y-4">
        <button
          onClick={onBack}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white text-xs font-semibold border border-slate-800 transition w-fit"
        >
          <ArrowLeft className="w-4 h-4 text-amber-400" />
          Voltar ao Painel Geral
        </button>
        <div className="flex flex-col items-center justify-center py-24 gap-4 text-center">
          <WifiOff className="w-10 h-10 text-rose-400" />
          <div>
            <p className="text-white font-semibold">Não foi possível carregar o detalhe do setor</p>
            <p className="text-slate-400 text-sm mt-1 max-w-md">{error}</p>
          </div>
          <button
            onClick={loadDetail}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-bold transition"
          >
            <RotateCcw className="w-4 h-4" />
            Tentar novamente
          </button>
        </div>
      </div>
    );
  }

  const isCritical = detailed.isCriticalLoss;
  const suspiciousCount = detailed.meters.filter((m) => m.suspicionStatus === 'SUSPEITO_BYPASS').length;

  return (
    <div className="space-y-6">

      {/* Top Navigation Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <button
          onClick={onBack}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white text-xs font-semibold border border-slate-800 transition w-fit"
        >
          <ArrowLeft className="w-4 h-4 text-amber-400" />
          Voltar ao Painel Geral
        </button>

        <div className="flex flex-wrap items-center gap-2">
          {/* PDF Audit Report Button (Available for all operators / auditors) */}
          <button
            onClick={handleGeneratePdf}
            disabled={isGeneratingPdf}
            id="btn-export-audit-pdf"
            className="px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 active:scale-[0.98] text-white text-xs font-bold flex items-center gap-2 shadow-sm transition cursor-pointer disabled:opacity-50"
            title="Gerar e descarregar relatório em PDF formatado para auditoria técnica da EDM"
          >
            {isGeneratingPdf ? (
              <Loader2 className="w-4 h-4 animate-spin text-white" />
            ) : (
              <FileDown className="w-4 h-4 text-white" />
            )}
            <span>{isGeneratingPdf ? 'A Gerar PDF...' : 'Gerar Relatório PDF'}</span>
          </button>

          {/* Admin Quick Action Controls */}
          {isAdmin && (
            <div className="flex flex-wrap items-center gap-2">
              {isCritical ? (
                <button
                  onClick={() => onNormalizeSector(detailed.transformerId)}
                  className="px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-sm transition"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  Regularizar Setor (3.5% Perda Técnica)
                </button>
              ) : (
                <button
                  onClick={() => onSimulateTheft(detailed.transformerId, 2400)}
                  className="px-3.5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-sm transition"
                >
                  <Flame className="w-4 h-4" />
                  Simular "Gato" (+2.400 kWh)
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* PDF Success Toast Banner */}
      {pdfMessage && (
        <div className="p-3.5 rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 text-xs font-medium flex items-center justify-between gap-3 animate-in fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{pdfMessage}</span>
          </div>
          <button
            onClick={() => setPdfMessage(null)}
            className="text-emerald-400 hover:text-emerald-200 text-xs px-2 py-0.5 rounded cursor-pointer"
          >
            Fechar
          </button>
        </div>
      )}

      {/* Main Sector Header Card */}
      <div className={`p-6 rounded-2xl border shadow-sm ${
        isCritical
          ? 'bg-gradient-to-r from-rose-950/40 via-slate-900 to-slate-900 border-rose-800/50'
          : 'bg-gradient-to-r from-emerald-950/40 via-slate-900 to-slate-900 border-emerald-800/50'
      }`}>
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2.5">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                isCritical ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
              }`}>
                {isCritical ? <ShieldAlert className="w-5 h-5" /> : <ShieldCheck className="w-5 h-5" />}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-xl sm:text-2xl font-black text-white tracking-tight">
                    {detailed.neighborhood}
                  </h1>
                  <span className="font-mono text-xs px-2.5 py-0.5 rounded-lg bg-slate-800 text-amber-400 border border-slate-700 font-bold">
                    {detailed.transformerCode}
                  </span>
                </div>
                <p className="text-xs text-slate-400 mt-0.5">
                  Capacidade Instalada: {detailed.capacityKva} kVA • {detailed.metersCount} medidores residenciais conectados
                </p>
              </div>
            </div>

            {/* Status Description Tag */}
            <div className="flex flex-wrap items-center gap-2 pt-1">
              <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ${
                isCritical
                  ? 'bg-rose-500/20 text-rose-300 border-rose-500/40 animate-pulse'
                  : 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
              }`}>
                {isCritical ? <AlertTriangle className="w-3.5 h-3.5" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
                {detailed.statusDescription}
              </span>

              {suspiciousCount > 0 && isCritical && (
                <span className="px-2.5 py-1 rounded-full bg-amber-500/15 text-amber-300 text-xs font-semibold border border-amber-500/30">
                  ⚠️ {suspiciousCount} medidor(es) com desvio crítico de consumo
                </span>
              )}
            </div>
          </div>

          {/* Large Metric Highlights */}
          <div className="flex items-center gap-4 shrink-0 bg-slate-950/70 p-4 rounded-xl border border-slate-800">
            <div>
              <span className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider block">
                Discrepância do Setor
              </span>
              <div className={`text-2xl sm:text-3xl font-black font-mono ${
                isCritical ? 'text-rose-400' : 'text-emerald-400'
              }`}>
                {detailed.lossPercentage.toFixed(1)}%
              </div>
              <span className="text-[10px] text-slate-500">
                Margem legal tolerada: {TECHNICAL_LOSS_THRESHOLD.toFixed(1)}%
              </span>
            </div>

            <div className="w-px h-12 bg-slate-800" />

            <div>
              <span className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider block">
                Volume Não Faturado
              </span>
              <div className="text-2xl sm:text-3xl font-black font-mono text-white">
                {detailed.lossKwh.toLocaleString('pt-MZ')} <span className="text-xs font-normal text-slate-400">kWh</span>
              </div>
              <span className="text-[10px] text-rose-400 font-semibold">
                ≈ {detailed.estimatedFinancialLossMt.toLocaleString('pt-MZ', { minimumFractionDigits: 2 })} MT
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Energy Balance Audit Formula Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-1 font-semibold">
            <span>(A) Injetado no Transformador</span>
            <Zap className="w-4 h-4 text-sky-400" />
          </div>
          <div className="text-xl font-bold font-mono text-sky-400">
            {detailed.totalSuppliedKwh.toLocaleString('pt-MZ')} <span className="text-xs font-normal">kWh</span>
          </div>
          <p className="text-[11px] text-slate-500 mt-1">
            Total lido no secundário do posto
          </p>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-1 font-semibold">
            <span>(B) Soma dos Medidores</span>
            <Home className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-xl font-bold font-mono text-emerald-400">
            {detailed.totalBilledKwh.toLocaleString('pt-MZ')} <span className="text-xs font-normal">kWh</span>
          </div>
          <p className="text-[11px] text-slate-500 mt-1">
            Total legalmente registrado nas residências
          </p>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-1 font-semibold">
            <span>(A - B) Perda do Circuito</span>
            <TrendingDown className={`w-4 h-4 ${isCritical ? 'text-rose-400' : 'text-slate-400'}`} />
          </div>
          <div className={`text-xl font-bold font-mono ${isCritical ? 'text-rose-400' : 'text-slate-200'}`}>
            {detailed.lossKwh.toLocaleString('pt-MZ')} <span className="text-xs font-normal">kWh ({detailed.lossPercentage.toFixed(1)}%)</span>
          </div>
          <p className="text-[11px] text-slate-500 mt-1">
            {isCritical ? 'Discrepância não técnica acentuada' : 'Perda dentro do limiar técnico'}
          </p>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-1 font-semibold">
            <span>Consumo Médio por Medidor</span>
            <Gauge className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-xl font-bold font-mono text-amber-400">
            {detailed.averageMeterConsumptionKwh.toLocaleString('pt-MZ')} <span className="text-xs font-normal">kWh</span>
          </div>
          <p className="text-[11px] text-slate-500 mt-1">
            Base de comparação para desvio de carga
          </p>
        </div>
      </div>

      {/* Connected Meters Table with Individualized Loss Calculations */}
      <div className="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm space-y-4 p-5">

        {/* Table Controls & Filters */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pb-3 border-b border-slate-800">
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Home className="w-4 h-4 text-amber-400" />
              Medidores Residenciais Conectados &amp; Auditoria Individualizada
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Exibição dos medidores, seus consumos faturados e o cálculo individualizado da perda pro-rata e desvio amostral.
            </p>
          </div>

          {/* Search and Filters */}
          <div className="flex flex-wrap items-center gap-2.5">
            {/* Search Box */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="Buscar medidor ou morador..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-8 pr-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white placeholder:text-slate-500 focus:outline-none focus:border-amber-500 w-48 sm:w-56"
              />
            </div>

            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
              aria-label="Filtrar por Diagnóstico"
              className="px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-300 focus:outline-none focus:border-amber-500"
            >
              <option value="ALL">Todos os Diagnósticos</option>
              <option value="SUSPEITO_BYPASS">⚠️ Apenas Suspeitos de Bypass</option>
              <option value="CONSUMO_ELEVADO">Consumo Elevado</option>
              <option value="REGULAR">Consumo Regular</option>
            </select>

            {/* Sort Dropdown */}
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              aria-label="Ordenar Medidores"
              className="px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-300 focus:outline-none focus:border-amber-500"
            >
              <option value="loss_desc">Maior Perda Alocada</option>
              <option value="billed_asc">Menor Consumo (Suspeitos)</option>
              <option value="billed_desc">Maior Consumo</option>
              <option value="number">Número do Medidor</option>
            </select>

            {/* Quick Export PDF Button */}
            <button
              onClick={handleGeneratePdf}
              disabled={isGeneratingPdf}
              className="px-3 py-1.5 bg-slate-950 hover:bg-slate-800 hover:text-white border border-slate-800 hover:border-amber-500/40 rounded-xl text-xs text-amber-400 font-medium flex items-center gap-1.5 transition cursor-pointer disabled:opacity-50"
              title="Descarregar Relatório de Auditoria em PDF"
            >
              {isGeneratingPdf ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-amber-400" />
              ) : (
                <FileDown className="w-3.5 h-3.5 text-amber-400" />
              )}
              <span>PDF</span>
            </button>
          </div>
        </div>

        {/* Meters Table */}
        <div className="overflow-x-auto rounded-xl border border-slate-800">
          <table className="w-full text-left text-xs text-slate-300 divide-y divide-slate-800">
            <thead className="bg-slate-950 text-[11px] uppercase tracking-wider text-slate-400">
              <tr>
                <th className="py-3 px-4 font-semibold">Medidor &amp; Morador</th>
                <th className="py-3 px-4 font-semibold">Endereço</th>
                <th className="py-3 px-4 font-semibold text-right">Consumo Faturado</th>
                <th className="py-3 px-4 font-semibold text-right">Participação Setor</th>
                <th className="py-3 px-4 font-semibold text-right">Perda Individual Alocada</th>
                <th className="py-3 px-4 font-semibold text-right">Prejuízo Pro-Rata</th>
                <th className="py-3 px-4 font-semibold text-right">Desvio da Média</th>
                <th className="py-3 px-4 font-semibold text-center">Diagnóstico do Ramal</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 bg-slate-950/40">
              {filteredMeters.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-slate-500 text-xs">
                    Nenhum medidor encontrado com os filtros aplicados.
                  </td>
                </tr>
              ) : (
                filteredMeters.map((m) => {
                  const isSuspicious = m.suspicionStatus === 'SUSPEITO_BYPASS';
                  const isHigh = m.suspicionStatus === 'CONSUMO_ELEVADO';

                  return (
                    <tr
                      key={m.id}
                      className={`hover:bg-slate-800/40 transition ${
                        isSuspicious ? 'bg-rose-950/15' : ''
                      }`}
                    >
                      {/* Meter & Resident */}
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <span className="font-mono font-bold text-amber-400">
                            {m.meterNumber}
                          </span>
                        </div>
                        <div className="text-white font-medium text-xs">
                          {m.residentName}
                        </div>
                        {m.phoneNumber && (
                          <div className="flex items-center gap-1 text-[11px] text-slate-400 font-mono mt-0.5">
                            <Phone className="w-3 h-3 text-slate-500 shrink-0" />
                            <span>{m.phoneNumber}</span>
                          </div>
                        )}
                      </td>

                      {/* Address */}
                      <td className="py-3 px-4 text-slate-400 max-w-[200px] truncate">
                        {m.address}
                      </td>

                      {/* Billed Consumption */}
                      <td className="py-3 px-4 text-right">
                        <span className="font-mono font-bold text-emerald-400 text-sm">
                          {m.billedKwh.toLocaleString('pt-MZ')} <span className="text-[11px] font-normal text-slate-400">kWh</span>
                        </span>
                        <div className="w-24 ml-auto bg-slate-800 rounded-full h-1 mt-1 overflow-hidden">
                          <div
                            className={`h-full ${isSuspicious ? 'bg-rose-500' : 'bg-emerald-500'}`}
                            style={{
                              width: `${Math.min(100, (m.billedKwh / (detailed.averageMeterConsumptionKwh * 1.5)) * 100)}%`
                            }}
                          />
                        </div>
                      </td>

                      {/* Share % */}
                      <td className="py-3 px-4 text-right font-mono text-slate-300">
                        {m.shareOfTotalBilledPercent.toFixed(1)}%
                      </td>

                      {/* Individual Loss (kWh) */}
                      <td className="py-3 px-4 text-right">
                        <span className={`font-mono font-bold ${
                          isCritical ? 'text-rose-400' : 'text-slate-300'
                        }`}>
                          {m.individualLossKwh.toLocaleString('pt-MZ')} <span className="text-[11px] font-normal text-slate-500">kWh</span>
                        </span>
                      </td>

                      {/* Individual Financial Loss */}
                      <td className="py-3 px-4 text-right font-mono text-slate-300">
                        {m.individualFinancialLossMt.toLocaleString('pt-MZ', { minimumFractionDigits: 2 })} MT
                      </td>

                      {/* Deviation from average */}
                      <td className="py-3 px-4 text-right font-mono">
                        <span className={m.deviationFromAverageKwh >= 0 ? 'text-emerald-400' : 'text-rose-400'}>
                          {m.deviationFromAverageKwh >= 0 ? '+' : ''}
                          {m.deviationFromAverageKwh.toLocaleString('pt-MZ')} kWh
                        </span>
                      </td>

                      {/* Suspicion Diagnosis Badge */}
                      <td className="py-3 px-4 text-center">
                        {isSuspicious ? (
                          <span
                            title={m.suspicionDescription}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 text-[10px] font-bold"
                          >
                            <AlertTriangle className="w-3 h-3 text-rose-400" />
                            Suspeita de Bypass
                          </span>
                        ) : isHigh ? (
                          <span
                            title={m.suspicionDescription}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-300 border border-sky-500/30 text-[10px] font-semibold"
                          >
                            Consumo Elevado
                          </span>
                        ) : (
                          <span
                            title={m.suspicionDescription}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 text-[10px] font-medium"
                          >
                            <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                            Regular
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Informative Explanation of Individual Loss Calculation */}
        <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 text-xs text-slate-400 flex items-start gap-2.5">
          <HelpCircle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
          <div className="space-y-0.5 text-[11px] leading-relaxed">
            <span className="font-bold text-white">Metodologia do Cálculo Individualizado de Perdas:</span>
            <p>
              A perda individual de cada ramal residencial é alocada via balanço pro-rata ponderado: <code className="text-amber-300 font-mono">Perda_Indiv = Perda_Total × (Consumo_Medidor / Soma_Total)</code>, valorizada na tarifa de referência EDM de {AVERAGE_TARIFF_MZN.toFixed(2)} MT/kWh. Residências em circuitos com perda &gt; 5% cujo consumo fique abaixo de 50% da média histórica do posto são categorizadas como <strong>Suspeita de Bypass/Gato</strong> para fiscalização em campo.
            </p>
          </div>
        </div>

      </div>

    </div>
  );
};
