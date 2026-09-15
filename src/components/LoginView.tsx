import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  Zap,
  Lock,
  User,
  KeyRound,
  AlertCircle,
  ArrowRight
} from 'lucide-react';

export const LoginView: React.FC = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      setError('Por favor, informe o usuário e a senha.');
      return;
    }

    setLoading(true);
    setError(null);

    const result = await login(username, password);
    setLoading(false);

    if (!result.success) {
      setError(result.message);
    }
  };

  const handleQuickLogin = async (u: string, p: string) => {
    setUsername(u);
    setPassword(p);
    setError(null);
    setLoading(true);
    const result = await login(u, p);
    setLoading(false);
    if (!result.success) {
      setError(result.message);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center items-center px-4 sm:px-6 lg:px-8 py-12 selection:bg-amber-500/20 selection:text-amber-300">
      
      {/* Background Subtle Glows */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[350px] bg-indigo-600/10 blur-[130px] rounded-full" />
        <div className="absolute bottom-1/4 right-1/3 w-[450px] h-[300px] bg-amber-500/5 blur-[120px] rounded-full" />
      </div>

      <div className="w-full max-w-md space-y-6 relative z-10">
        
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-slate-900 border border-amber-500/30 text-amber-400 shadow-xl shadow-amber-500/5 mb-2">
            <Zap className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            Auditoria Energética
          </h1>
          <p className="text-xs text-slate-400 max-w-sm mx-auto leading-relaxed">
            Detecção de Perdas Não Técnicas &amp; Fraudes em Redes de Distribuição Elétrica
          </p>
        </div>

        {/* Login Card */}
        <div className="rounded-2xl bg-slate-900/90 border border-slate-800 shadow-2xl p-6 sm:p-8 backdrop-blur-xl">
          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Error Message */}
            {error && (
              <div className="p-3 rounded-xl bg-rose-950/40 border border-rose-800/60 text-rose-300 text-xs flex items-start gap-2.5">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                <span className="leading-snug">{error}</span>
              </div>
            )}

            {/* Username Input */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Usuário
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <User className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  required
                  placeholder="genserico, oxidia, leysa ou helen"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-950 border border-slate-700/80 rounded-xl text-sm text-white placeholder:text-slate-500 focus:outline-none focus:border-amber-500 transition font-mono"
                />
              </div>
            </div>

            {/* Password Input */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Senha
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type="password"
                  required
                  placeholder="Ver credenciais no botão de acesso rápido"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-950 border border-slate-700/80 rounded-xl text-sm text-white placeholder:text-slate-500 focus:outline-none focus:border-amber-500 transition font-mono"
                />
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 px-4 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold text-sm shadow-md transition disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer mt-2"
            >
              {loading ? (
                <div className="w-4 h-4 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <KeyRound className="w-4 h-4" />
                  Acessar Dashboard
                </>
              )}
            </button>
          </form>

          {/* Quick Access Profiles */}
          <div className="mt-6 pt-5 border-t border-slate-800/80 space-y-3">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block text-center">
              Acesso Rápido para Apresentação
            </span>

            {/* Admins */}
            <div className="space-y-1.5">
              <span className="text-[10px] font-semibold text-amber-400/90 uppercase tracking-wider flex items-center gap-1">
                👑 Administradores (Senha: EdmMz#2026!Adm)
              </span>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => handleQuickLogin('genserico', 'EdmMz#2026!Adm')}
                  className="p-2.5 rounded-xl bg-slate-950 hover:bg-slate-800/80 border border-amber-500/30 hover:border-amber-500/60 text-left transition group cursor-pointer"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-amber-300 group-hover:text-amber-200">
                      Genserico
                    </span>
                    <ArrowRight className="w-3 h-3 text-slate-500 group-hover:text-amber-400 transition" />
                  </div>
                  <span className="text-[10px] text-slate-400 block mt-0.5">Admin Geral</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleQuickLogin('oxidia', 'EdmMz#2026!Adm')}
                  className="p-2.5 rounded-xl bg-slate-950 hover:bg-slate-800/80 border border-amber-500/30 hover:border-amber-500/60 text-left transition group cursor-pointer"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-amber-300 group-hover:text-amber-200">
                      Oxidia
                    </span>
                    <ArrowRight className="w-3 h-3 text-slate-500 group-hover:text-amber-400 transition" />
                  </div>
                  <span className="text-[10px] text-slate-400 block mt-0.5">Admin Geral</span>
                </button>
              </div>
            </div>

            {/* Operators */}
            <div className="space-y-1.5 pt-1">
              <span className="text-[10px] font-semibold text-sky-400/90 uppercase tracking-wider flex items-center gap-1">
                👷 Operadores Técnicos (Senha: EdmMz#2026!Op)
              </span>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => handleQuickLogin('leysa', 'EdmMz#2026!Op')}
                  className="p-2.5 rounded-xl bg-slate-950 hover:bg-slate-800/80 border border-slate-700 hover:border-sky-500/50 text-left transition group cursor-pointer"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-200 group-hover:text-sky-300">
                      Leysa
                    </span>
                    <ArrowRight className="w-3 h-3 text-slate-500 group-hover:text-sky-400 transition" />
                  </div>
                  <span className="text-[10px] text-slate-400 block mt-0.5">Operadora</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleQuickLogin('helen', 'EdmMz#2026!Op')}
                  className="p-2.5 rounded-xl bg-slate-950 hover:bg-slate-800/80 border border-slate-700 hover:border-sky-500/50 text-left transition group cursor-pointer"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-200 group-hover:text-sky-300">
                      Helen
                    </span>
                    <ArrowRight className="w-3 h-3 text-slate-500 group-hover:text-sky-400 transition" />
                  </div>
                  <span className="text-[10px] text-slate-400 block mt-0.5">Operadora</span>
                </button>
              </div>
            </div>
          </div>

        </div>

      </div>

    </div>
  );
};
