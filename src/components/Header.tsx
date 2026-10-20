import React, { useState } from 'react';
import { Zap, ShieldAlert, RotateCcw, LogOut, User, Shield, KeyRound, Users } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { PasswordChangeDialog } from './PasswordChangeDialog';
import { AdminAccountsDialog } from './AdminAccountsDialog';

interface HeaderProps {
  onResetData: () => void;
  criticalCount: number;
}

export const Header: React.FC<HeaderProps> = ({
  onResetData,
  criticalCount
}) => {
  const { user, logout, isAdmin } = useAuth();
  const [changingPassword, setChangingPassword] = useState(false);
  const [administering, setAdministering] = useState(false);

  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur-md sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        
        {/* Brand & Identity */}
        <div className="flex items-center space-x-3.5">
          <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 shadow-inner">
            <Zap className="w-5 h-5 fill-amber-400 text-amber-400" />
          </div>
          <div>
            <div className="flex items-center space-x-2.5">
              <h1 className="text-lg font-bold tracking-tight text-white flex items-center gap-2">
                GridAudit
                <span className="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-medium border border-slate-700">
                  Perdas Não Técnicas
                </span>
              </h1>
              {criticalCount > 0 && (
                <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/25 animate-pulse font-medium">
                  <ShieldAlert className="w-3 h-3" />
                  {criticalCount} {criticalCount === 1 ? 'Alerta Crítico' : 'Alertas Críticos'}
                </span>
              )}
            </div>
            <p className="text-xs text-slate-400">
              Análise de perdas por transformador • Limiar demonstrativo de 5%
            </p>
          </div>
        </div>

        {/* Action Controls & Navigation */}
        <div className="flex flex-wrap items-center gap-2.5">
          <button
            id="reset-data-btn"
            onClick={onResetData}
            title="Recarregar dados do servidor"
            className="p-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-white border border-slate-700/60 transition"
          >
            <RotateCcw className="w-4 h-4" />
          </button>

          {/* User Profile Pill & Logout */}
          {user && (
            <div className="flex items-center pl-2 border-l border-slate-800 gap-2">
              <div className="flex items-center gap-2 px-2.5 py-1 rounded-xl bg-slate-950 border border-slate-800">
                <div className={`w-6 h-6 rounded-lg flex items-center justify-center text-xs font-bold ${
                  isAdmin ? 'bg-amber-500/20 text-amber-400' : 'bg-slate-700 text-slate-300'
                }`}>
                  {isAdmin ? <Shield className="w-3.5 h-3.5" /> : <User className="w-3.5 h-3.5" />}
                </div>
                <div className="text-left leading-none">
                  <span className="text-xs font-bold text-white block capitalize">
                    {user.name || user.username}
                  </span>
                  <span className={`text-[9px] font-semibold uppercase tracking-wider ${
                    isAdmin ? 'text-amber-400' : 'text-slate-400'
                  }`}>
                    {isAdmin ? 'ADMIN' : 'OPERADOR'}
                  </span>
                </div>
              </div>

              <button
                onClick={() => setChangingPassword(true)}
                title="Alterar password"
                aria-label="Alterar password"
                className="p-1.5 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-400 border border-slate-700/60"
              >
                <KeyRound className="w-4 h-4" />
              </button>
              {isAdmin && <button type="button" onClick={() => setAdministering(true)} title="Administração de contas" aria-label="Administração de contas"
                className="p-1.5 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-400 border border-slate-700/60"><Users className="w-4 h-4" /></button>}
              <button
                onClick={logout}
                title="Encerrar Sessão (Logout)"
                className="p-1.5 rounded-xl bg-slate-800/80 hover:bg-rose-950/60 hover:text-rose-400 text-slate-400 border border-slate-700/60 transition"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>

      </div>
      {changingPassword && <PasswordChangeDialog onClose={() => setChangingPassword(false)} />}
      {administering && <AdminAccountsDialog onClose={() => setAdministering(false)} />}
    </header>
  );
};
