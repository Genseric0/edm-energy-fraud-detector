import React, { useState } from 'react';
import { TransformerAnalysis } from '../types/energy';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  ReferenceLine,
  LineChart,
  Line
} from 'recharts';
import { BarChart3, LineChart as LineChartIcon, ShieldAlert } from 'lucide-react';

interface DiscrepancyChartProps {
  transformers: TransformerAnalysis[];
  onSelectTransformer: (transformer: TransformerAnalysis) => void;
}

export const DiscrepancyChart: React.FC<DiscrepancyChartProps> = ({
  transformers,
  onSelectTransformer
}) => {
  const [viewMode, setViewMode] = useState<'volume' | 'percentage'>('volume');

  const chartData = transformers.map((t) => ({
    id: t.transformerId,
    code: t.transformerCode,
    name: t.neighborhood,
    shortName: t.neighborhood.length > 14 ? t.neighborhood.substring(0, 12) + '...' : t.neighborhood,
    fornecido: t.totalSuppliedKwh,
    faturado: t.totalBilledKwh,
    perda: t.lossKwh,
    lossPercentage: t.lossPercentage,
    isCritical: t.isCriticalLoss,
    raw: t
  }));

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="bg-slate-900/95 border border-slate-700 rounded-xl p-3 shadow-xl backdrop-blur-md text-xs">
          <div className="flex items-center justify-between gap-3 mb-2 border-b border-slate-800 pb-1.5">
            <span className="font-bold text-white text-sm">{data.name}</span>
            <span className="font-mono text-slate-400">{data.code}</span>
          </div>

          <div className="space-y-1 text-slate-300">
            <div className="flex justify-between gap-4">
              <span className="text-sky-400">Fornecido (Transformador):</span>
              <strong className="text-white font-mono">{data.fornecido.toLocaleString('pt-MZ')} kWh</strong>
            </div>
            <div className="flex justify-between gap-4">
              <span className="text-emerald-400">Faturado (Medidores):</span>
              <strong className="text-white font-mono">{data.faturado.toLocaleString('pt-MZ')} kWh</strong>
            </div>
            <div className="flex justify-between gap-4">
              <span className="text-rose-400">Perda / Desvio:</span>
              <strong className="text-rose-400 font-mono">{data.perda.toLocaleString('pt-MZ')} kWh</strong>
            </div>
            <div className="flex justify-between gap-4 pt-1 border-t border-slate-800 font-semibold">
              <span>% de Perda:</span>
              <span className={data.isCritical ? 'text-rose-400 font-bold' : 'text-emerald-400 font-bold'}>
                {data.lossPercentage.toFixed(1)}% {data.isCritical ? '(Acima do Limite de 5%)' : '(Dentro da tolerância)'}
              </span>
            </div>
          </div>

          <div className="mt-2 pt-2 border-t border-slate-800 flex items-center justify-between text-[11px]">
            <span className={`inline-flex items-center gap-1 font-medium ${data.isCritical ? 'text-rose-400' : 'text-emerald-400'}`}>
              {data.isCritical && <ShieldAlert className="w-3 h-3" />}
              {data.isCritical ? 'Discrepância a investigar' : 'Abaixo do limiar demonstrativo'}
            </span>
            <span className="text-slate-500">Clique na barra para detalhes</span>
          </div>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-sm">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <div>
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            Discrepância Energética por Transformador
          </h2>
          <p className="text-xs text-slate-400">
            Comparativo entre energia injetada na rede e consumo total faturado pelos medidores
          </p>
        </div>

        {/* View mode toggle */}
        <div className="flex items-center bg-slate-950 p-1 rounded-lg border border-slate-800 self-start sm:self-auto">
          <button
            onClick={() => setViewMode('volume')}
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium transition ${
              viewMode === 'volume'
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <BarChart3 className="w-3.5 h-3.5 text-sky-400" />
            Volume (kWh)
          </button>
          <button
            onClick={() => setViewMode('percentage')}
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium transition ${
              viewMode === 'percentage'
                ? 'bg-slate-800 text-white shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <LineChartIcon className="w-3.5 h-3.5 text-rose-400" />
            Taxa de Perda (%)
          </button>
        </div>
      </div>

      <div className="h-72 w-full">
        <ResponsiveContainer width="100%" height="100%">
          {viewMode === 'volume' ? (
            <BarChart
              data={chartData}
              margin={{ top: 10, right: 10, left: -10, bottom: 25 }}
              onClick={(state: any) => {
                if (state && state.activePayload && state.activePayload[0]) {
                  onSelectTransformer(state.activePayload[0].payload.raw);
                }
              }}
              className="cursor-pointer"
            >
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
              <XAxis
                dataKey="shortName"
                stroke="#64748b"
                tick={{ fill: '#94a3b8', fontSize: 11 }}
                angle={-15}
                textAnchor="end"
                interval={0}
              />
              <YAxis
                stroke="#64748b"
                tick={{ fill: '#94a3b8', fontSize: 11 }}
                tickFormatter={(val) => `${(val / 1000).toFixed(0)}k`}
              />
              <Tooltip content={<CustomTooltip />} />
              <Legend
                wrapperStyle={{ paddingTop: 8, fontSize: '12px' }}
                formatter={(value) => {
                  if (value === 'fornecido') return <span className="text-slate-300">Fornecido pelo Transformador (kWh)</span>;
                  if (value === 'faturado') return <span className="text-slate-300">Faturado pelos Medidores (kWh)</span>;
                  if (value === 'perda') return <span className="text-slate-300">Perda de Energia (kWh)</span>;
                  return value;
                }}
              />
              <Bar isAnimationActive={false} dataKey="fornecido" fill="#0284c7" radius={[4, 4, 0, 0]} maxBarSize={38} />
              <Bar isAnimationActive={false} dataKey="faturado" fill="#059669" radius={[4, 4, 0, 0]} maxBarSize={38} />
              <Bar isAnimationActive={false} dataKey="perda" fill="#e11d48" radius={[4, 4, 0, 0]} maxBarSize={38} />
            </BarChart>
          ) : (
            <LineChart
              data={chartData}
              margin={{ top: 10, right: 20, left: -10, bottom: 25 }}
              onClick={(state: any) => {
                if (state && state.activePayload && state.activePayload[0]) {
                  onSelectTransformer(state.activePayload[0].payload.raw);
                }
              }}
              className="cursor-pointer"
            >
              <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
              <XAxis
                dataKey="shortName"
                stroke="#64748b"
                tick={{ fill: '#94a3b8', fontSize: 11 }}
                angle={-15}
                textAnchor="end"
                interval={0}
              />
              <YAxis
                stroke="#64748b"
                tick={{ fill: '#94a3b8', fontSize: 11 }}
                tickFormatter={(val) => `${val}%`}
              />
              <Tooltip content={<CustomTooltip />} />
              <ReferenceLine
                y={5.0}
                stroke="#f43f5e"
                strokeDasharray="4 4"
                label={{
                  value: 'Limiar demonstrativo: 5.0%',
                  fill: '#fb7185',
                  fontSize: 11,
                  position: 'insideTopRight'
                }}
              />
              <Line
                isAnimationActive={false}
                type="monotone"
                dataKey="lossPercentage"
                stroke="#f43f5e"
                strokeWidth={2.5}
                dot={{ r: 5, fill: '#f43f5e' }}
                activeDot={{ r: 7, stroke: '#fff', strokeWidth: 2 }}
                name="% Perda Real"
              />
            </LineChart>
          )}
        </ResponsiveContainer>
      </div>

      <div className="mt-2 text-center text-xs text-slate-500">
        💡 Dica: Clique em qualquer barra para abrir o diagnóstico detalhado e inspecionar os medidores daquele setor.
      </div>
    </div>
  );
};
