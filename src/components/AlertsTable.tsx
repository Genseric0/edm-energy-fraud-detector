import React, { useState } from 'react';
import { TransformerAnalysis } from '../types/energy';
import {
  ShieldAlert,
  ShieldCheck,
  Search,
  SlidersHorizontal,
  ChevronRight,
  Flame,
  CheckCircle2,
  AlertOctagon
} from 'lucide-react';

interface AlertsTableProps {
  transformers: TransformerAnalysis[];
  onSelectTransformer: (transformer: TransformerAnalysis) => void;
  onSimulateTheft: (transformerId: number) => void;
  onNormalizeSector: (transformerId: number) => void;
}

export const AlertsTable: React.FC<AlertsTableProps> = ({
  transformers,
  onSelectTransformer,
  onSimulateTheft,
  onNormalizeSector
}) => {
  const [filter, setFilter] = useState<'ALL' | 'CRITICAL' | 'NORMAL'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const filteredTransformers = transformers.filter((t) => {
    const matchesSearch =
      t.neighborhood.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.transformerCode.toLowerCase().includes(searchTerm.toLowerCase());

    if (!matchesSearch) return false;
    if (filter === 'CRITICAL') return t.isCriticalLoss;
    if (filter === 'NORMAL') return !t.isCriticalLoss;
    return true;
  });

  const criticalCount = transformers.filter((t) => t.isCriticalLoss).length;
  const normalCount = transformers.length - criticalCount;

  return (
    <div className="rounded-2xl bg-slate-900/80 border border-slate-800 shadow-sm overflow-hidden">
      
      {/* Table Header Controls */}
      <div className="p-4 sm:p-5 border-b border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-base font-bold text-white">
              Tabela de Auditoria e Alertas de Balanço Energético
            </h2>
            <span className="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">
              {filteredTransformers.length} de {transformers.length}
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Classificação automatizada baseada na margem de tolerância técnica de 5% da energia fornecida
          </p>
        </div>

        {/* Filters and Search */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5">
          {/* Search Box */}
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Buscar bairro ou código..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9 pr-3 py-1.5 bg-slate-950 border border-slate-800 rounded-lg text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-amber-500/50 w-full sm:w-56"
            />
          </div>

          {/* Filter Tabs */}
          <div className="flex items-center bg-slate-950 p-1 rounded-lg border border-slate-800 self-start sm:self-auto">
            <button
              onClick={() => setFilter('ALL')}
              className={`px-2.5 py-1 rounded-md text-xs font-medium transition ${
                filter === 'ALL'
                  ? 'bg-slate-800 text-white font-semibold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Todos ({transformers.length})
            </button>
            <button
              onClick={() => setFilter('CRITICAL')}
              className={`flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium transition ${
                filter === 'CRITICAL'
                  ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30 font-semibold'
                  : 'text-rose-400 hover:text-rose-300'
              }`}
            >
              <ShieldAlert className="w-3 h-3" />
              Fraude ({criticalCount})
            </button>
            <button
              onClick={() => setFilter('NORMAL')}
              className={`flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium transition ${
                filter === 'NORMAL'
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 font-semibold'
                  : 'text-emerald-400 hover:text-emerald-300'
              }`}
            >
              <ShieldCheck className="w-3 h-3" />
              Normal ({normalCount})
            </button>
          </div>
        </div>
      </div>

      {/* Responsive Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs text-slate-300">
          <thead className="bg-slate-950/60 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
            <tr>
              <th className="py-3 px-4 font-semibold">Transformador / Bairro</th>
              <th className="py-3 px-4 font-semibold text-center">Medidores</th>
              <th className="py-3 px-4 font-semibold text-right">Fornecido</th>
              <th className="py-3 px-4 font-semibold text-right">Faturado</th>
              <th className="py-3 px-4 font-semibold text-right">Discrepância</th>
              <th className="py-3 px-4 font-semibold">Taxa de Perda (%)</th>
              <th className="py-3 px-4 font-semibold">Classificação do Setor</th>
              <th className="py-3 px-4 font-semibold text-center">Ações</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {filteredTransformers.length === 0 ? (
              <tr>
                <td colSpan={8} className="py-8 text-center text-slate-500">
                  Nenhum transformador encontrado com os filtros aplicados.
                </td>
              </tr>
            ) : (
              filteredTransformers.map((item) => {
                const isCritical = item.isCriticalLoss;
                return (
                  <tr
                    key={item.transformerId}
                    className={`hover:bg-slate-800/40 transition group ${
                      isCritical ? 'bg-rose-950/10' : ''
                    }`}
                  >
                    {/* Transformer & Neighborhood */}
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-white text-sm">
                        {item.neighborhood}
                      </div>
                      <div className="flex items-center gap-2 mt-0.5 text-slate-400 font-mono text-[11px]">
                        <span className="px-1.5 py-0.2 rounded bg-slate-800 border border-slate-700">
                          {item.transformerCode}
                        </span>
                        <span>{item.capacityKva} kVA</span>
                      </div>
                    </td>

                    {/* Meters count */}
                    <td className="py-3.5 px-4 text-center">
                      <span className="px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-medium">
                        {item.metersCount} medidores
                      </span>
                    </td>

                    {/* Total Supplied */}
                    <td className="py-3.5 px-4 text-right font-mono font-medium text-slate-200">
                      {item.totalSuppliedKwh.toLocaleString('pt-MZ')} kWh
                    </td>

                    {/* Total Billed */}
                    <td className="py-3.5 px-4 text-right font-mono font-medium text-emerald-400">
                      {item.totalBilledKwh.toLocaleString('pt-MZ')} kWh
                    </td>

                    {/* Loss Discrepancy */}
                    <td className="py-3.5 px-4 text-right font-mono">
                      <div className={`font-semibold ${isCritical ? 'text-rose-400' : 'text-slate-300'}`}>
                        {item.lossKwh.toLocaleString('pt-MZ')} kWh
                      </div>
                      {isCritical && item.estimatedFinancialLossMt > 0 && (
                        <div className="text-[10px] text-rose-400/80">
                          ~ {item.estimatedFinancialLossMt.toLocaleString('pt-MZ', { minimumFractionDigits: 2 })} MT
                        </div>
                      )}
                    </td>

                    {/* Loss % with progress bar */}
                    <td className="py-3.5 px-4 min-w-[150px]">
                      <div className="flex items-center justify-between text-xs mb-1">
                        <span className={`font-bold font-mono ${isCritical ? 'text-rose-400' : 'text-emerald-400'}`}>
                          {item.lossPercentage.toFixed(1)}%
                        </span>
                        <span className="text-[10px] text-slate-500">
                          {item.lossPercentage <= 5 ? '≤ 5.0%' : '> 5.0%'}
                        </span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden relative">
                        {/* 5% technical threshold tick */}
                        <div
                          className="absolute top-0 bottom-0 w-0.5 bg-amber-400/80 z-10"
                          style={{ left: '10%' }}
                          title="Limite tolerável: 5%"
                        />
                        <div
                          className={`h-full rounded-full transition-all duration-500 ${
                            isCritical ? 'bg-rose-500' : 'bg-emerald-500'
                          }`}
                          style={{ width: `${Math.min(100, item.lossPercentage * 2)}%` }}
                        />
                      </div>
                    </td>

                    {/* Status Badge */}
                    <td className="py-3.5 px-4">
                      {isCritical ? (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-rose-500/15 border border-rose-500/30 text-rose-300 font-medium">
                          <AlertOctagon className="w-3.5 h-3.5 text-rose-400 shrink-0 animate-pulse" />
                          <span>Alerta Crítico: Suspeita de Ligação Clandestina</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 font-medium">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                          <span>Normal: Perda Técnica Aceitável</span>
                        </span>
                      )}
                    </td>

                    {/* Actions */}
                    <td className="py-3.5 px-4 text-center">
                      <div className="flex items-center justify-center gap-1.5">
                        <button
                          onClick={() => onSelectTransformer(item)}
                          className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white font-medium flex items-center gap-1 transition"
                          title="Ver medidores e detalhes deste setor"
                        >
                          <span>Medidores</span>
                          <ChevronRight className="w-3 h-3 text-slate-400" />
                        </button>

                        {isCritical ? (
                          <button
                            onClick={() => onNormalizeSector(item.transformerId)}
                            className="p-1 rounded-lg bg-emerald-950 hover:bg-emerald-900 text-emerald-300 border border-emerald-800/60 transition"
                            title="Simular regularização da rede (reduzir perda para normal)"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" />
                          </button>
                        ) : (
                          <button
                            onClick={() => onSimulateTheft(item.transformerId)}
                            className="p-1 rounded-lg bg-rose-950 hover:bg-rose-900 text-rose-300 border border-rose-800/60 transition"
                            title="Simular ligação clandestina (injetar furto de carga)"
                          >
                            <Flame className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </td>

                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

    </div>
  );
};
