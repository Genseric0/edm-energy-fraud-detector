import React, { useState, useEffect, useCallback, useRef } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LoginView } from './components/LoginView';
import { Header } from './components/Header';
import { SummaryCards } from './components/SummaryCards';
import { DiscrepancyChart } from './components/DiscrepancyChart';
import { AlertsTable } from './components/AlertsTable';
import { TransformerDetailView } from './components/TransformerDetailView';
import { energyApi } from './api/energyApi';
import { ApiError } from './api/client';
import { TransformerAnalysis, DashboardSummary } from './types/energy';
import { Loader2, WifiOff, RotateCcw, Info, Database } from 'lucide-react';

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}

function MainApp() {
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();

  // Enquanto a sessão está a ser validada contra o backend (GET /api/auth/me),
  // evita mostrar o ecrã de login por um instante antes de confirmar que já há sessão.
  if (isAuthLoading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center">
        <Loader2 className="w-8 h-8 text-amber-400 animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginView />;
  }

  return <Dashboard />;
}

function Dashboard() {
  const { isAdmin } = useAuth();

  const [transformers, setTransformers] = useState<TransformerAnalysis[]>([]);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [selectedTransformerId, setSelectedTransformerId] = useState<number | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshWarning, setRefreshWarning] = useState<string | null>(null);
  const requestVersion = useRef(0);

  const loadData = useCallback(async () => {
    const version = ++requestVersion.current;
    setIsLoading(true);
    setError(null);
    try {
      const [transformersData, summaryData] = await Promise.all([
        energyApi.getAllTransformers(),
        energyApi.getSummary(),
      ]);
      if (version !== requestVersion.current) return;
      setTransformers(transformersData);
      setSummary(summaryData);
      setRefreshWarning(null);
    } catch (err) {
      const message =
        err instanceof ApiError
          ? err.message
          : 'Erro inesperado ao carregar os dados do servidor.';
      if (version === requestVersion.current) setError(message);
    } finally {
      if (version === requestVersion.current) setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
    return () => { requestVersion.current++; };
  }, [loadData]);

  const selectedTransformer = transformers.find((t) => t.transformerId === selectedTransformerId) ?? null;

  // Simula ligação clandestina: chama o backend real, que recalcula o balanço energético.
  const handleSimulateTheft = async (transformerId: number, additionalKwh = 2400) => {
    try {
      const updated = await energyApi.simulateTheft(transformerId, additionalKwh);
      applyUpdatedTransformer(updated);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Erro ao simular ligação clandestina.';
      setError(message);
    }
  };

  // Regulariza o setor: chama o backend real, que ajusta o fornecimento para o patamar técnico.
  const handleNormalizeSector = async (transformerId: number) => {
    try {
      const updated = await energyApi.normalizeSector(transformerId);
      applyUpdatedTransformer(updated);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Erro ao regularizar o setor.';
      setError(message);
    }
  };

  const applyUpdatedTransformer = (updated: TransformerAnalysis) => {
    setTransformers((prev) =>
      prev.map((t) => (t.transformerId === updated.transformerId ? updated : t))
    );
    // O resumo agregado (totais, contagem de alertas) depende de todos os transformadores,
    // por isso é recarregado do backend em vez de recalculado no cliente.
    energyApi.getSummary().then(data => { setSummary(data); setRefreshWarning(null); }).catch(() => {
      setRefreshWarning('O sector foi actualizado, mas o resumo pode estar desactualizado. Recarregue os dados.');
    });
  };

  const criticalCount = summary?.criticalAlertsCount ?? 0;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-amber-500/20 selection:text-amber-300">
      <Header
        onResetData={loadData}
        criticalCount={criticalCount}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div><p className="text-xs uppercase tracking-wider text-amber-400 font-semibold">GridAudit</p><h2 className="text-2xl font-bold mt-1">Balanço de energia</h2><p className="text-sm text-slate-400 mt-1">Compare a rede, identifique discrepâncias e apoie a investigação técnica.</p></div>
          <span className="rounded-full border border-amber-700/60 bg-amber-950/30 px-3 py-1 text-xs text-amber-200">Protótipo · parâmetros demonstrativos</span>
        </div>
        {refreshWarning && <div role="alert" className="rounded-xl border border-amber-700 bg-amber-950/30 p-4 text-sm text-amber-200">{refreshWarning}</div>}
        <section role="note" aria-label="Cobertura e limitações" className="rounded-2xl border border-sky-800/60 bg-sky-950/30 p-4 sm:p-5">
          <div className="flex items-start gap-3">
            <Info className="w-5 h-5 text-sky-300 shrink-0 mt-0.5" />
            <div className="space-y-2">
              <h2 className="text-sm font-bold text-sky-200">Cobertura e limites da detecção</h2>
              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                O sistema compara a energia medida no transformador com a energia registada pelos contadores.
                A fiscalização é mais fiável com contadores acessíveis na rede/postes, telemedição e medições de qualidade.
                Contadores dentro das residências também integram o balanço do sector, mas identificar um bypass individual
                pode ser difícil sem sensores adicionais, histórico e inspecção técnica.
              </p>
              <p className="text-xs text-sky-200/90">
                Alertas não provam fraude. Dados sintéticos; estimativas não são medições individuais nem multas.
                Limiar de 5% e valor de 7,50 MT/kWh são demonstrativos, sem validação oficial.
              </p>
            </div>
          </div>
        </section>
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-24 gap-3 text-slate-400">
            <Loader2 className="w-8 h-8 text-amber-400 animate-spin" />
            <span className="text-sm">A carregar dados da rede de distribuição...</span>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center py-24 gap-4 text-center">
            <WifiOff className="w-10 h-10 text-rose-400" />
            <div>
              <p className="text-white font-semibold">Não foi possível carregar os dados</p>
              <p className="text-slate-400 text-sm mt-1 max-w-md">{error}</p>
            </div>
            <button
              onClick={loadData}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-bold transition"
            >
              <RotateCcw className="w-4 h-4" />
              Tentar novamente
            </button>
          </div>
        ) : selectedTransformer ? (
          <TransformerDetailView
            transformer={selectedTransformer}
            onBack={() => setSelectedTransformerId(null)}
            onSimulateTheft={handleSimulateTheft}
            onNormalizeSector={handleNormalizeSector}
            isAdmin={isAdmin}
          />
        ) : transformers.length === 0 ? (
          <section aria-label="Rede sem dados" className="rounded-2xl border border-slate-700 bg-slate-900/70 px-6 py-14 text-center">
            <Database className="mx-auto h-10 w-10 text-slate-500" />
            <h2 className="mt-4 text-lg font-bold">Ainda não há transformadores</h2>
            <p className="mx-auto mt-2 max-w-lg text-sm text-slate-400">Esta instalação não tem dados de rede. Consulte o responsável pela configuração antes de introduzir dados reais.</p>
            <button type="button" onClick={loadData} className="mt-5 rounded-lg bg-slate-800 px-4 py-3 text-sm font-semibold text-white">Actualizar dados</button>
          </section>
        ) : (
          <>
            {summary && <SummaryCards summary={summary} />}

            <DiscrepancyChart
              transformers={transformers}
              onSelectTransformer={(t) => setSelectedTransformerId(t.transformerId)}
            />

            <AlertsTable
              transformers={transformers}
              onSelectTransformer={(t) => setSelectedTransformerId(t.transformerId)}
              onSimulateTheft={(id) => handleSimulateTheft(id)}
              onNormalizeSector={(id) => handleNormalizeSector(id)}
            />
          </>
        )}
      </main>

      <footer className="border-t border-slate-900 bg-slate-950 py-4 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>GridAudit • Projecto independente, sem afiliação à EDM</span>
          <span className="text-slate-500">Maputo, Moçambique</span>
        </div>
      </footer>
    </div>
  );
}
