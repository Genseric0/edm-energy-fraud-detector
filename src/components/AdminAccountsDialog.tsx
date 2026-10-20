import React, { useEffect, useRef, useState } from 'react';
import { AdminAccount, AccountPage, adminAccountsApi } from '../api/adminAccountsApi';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';

type Action = 'edit' | 'revoke' | 'unlock';
export function AdminAccountsDialog({ onClose }: { onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const { user, logout } = useAuth();
  const [data, setData] = useState<AccountPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [selected, setSelected] = useState<AdminAccount | null>(null);
  const [action, setAction] = useState<Action>('edit');
  const [role, setRole] = useState<AdminAccount['role']>('OPERATOR');
  const [active, setActive] = useState(true);
  const [reason, setReason] = useState('');
  const [password, setPassword] = useState('');

  async function load(page: number) {
    setLoading(true);
    try { setData(await adminAccountsApi.list(page)); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Não foi possível carregar as contas.'); }
    finally { setLoading(false); }
  }
  useEffect(() => { dialog.current?.showModal(); void load(0); }, []);

  function select(account: AdminAccount, nextAction: Action) {
    setSelected(account); setAction(nextAction); setRole(account.role); setActive(account.active);
    setReason(''); setPassword(''); setError(''); setNotice('');
  }
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!selected || busy) return;
    setBusy(true); setError(''); setNotice('');
    const input = { expectedVersion: selected.version, reason, currentPassword: password };
    try {
      if (action === 'edit') await adminAccountsApi.update(selected.id, { ...input, role, active });
      else if (action === 'revoke') await adminAccountsApi.revoke(selected.id, input);
      else await adminAccountsApi.unlock(selected.id, input);
      setPassword(''); setSelected(null);
      if (selected.username === user?.username) { logout(); return; }
      setNotice(`Operação concluída para ${selected.username}. Sessões anteriores invalidadas.`);
      await load(data?.page ?? 0);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível guardar a alteração.');
      setPassword('');
      if (err instanceof ApiError && err.status === 409) {
        setSelected(null); await load(data?.page ?? 0);
      }
    } finally { setBusy(false); }
  }

  return (
    <dialog ref={dialog} aria-labelledby="accounts-title" onCancel={onClose}
      className="m-auto w-11/12 max-w-5xl max-h-[90dvh] overflow-y-auto rounded-2xl border border-slate-700 bg-slate-900 p-5 text-slate-100 backdrop:bg-slate-950/80">
      <div className="flex items-start justify-between gap-3 mb-4">
        <div><h2 id="accounts-title" className="text-lg font-bold">Administração de contas</h2>
          <p className="text-sm text-slate-400 mt-1">Gerir contas existentes. Alterações exigem motivo e password actual e invalidam as sessões da conta afectada.</p></div>
        <button autoFocus type="button" onClick={onClose} disabled={busy} className="rounded-lg border border-slate-700 px-3 py-2">Fechar</button>
      </div>
      {error && <p role="alert" className="mb-3 rounded-lg bg-rose-950/40 p-3 text-sm text-rose-300">{error}</p>}
      {notice && <p role="status" className="mb-3 rounded-lg bg-emerald-950/40 p-3 text-sm text-emerald-300">{notice}</p>}
      {loading ? <p role="status">A carregar contas…</p> : !data ? (
        <button onClick={() => { setError(''); void load(0); }}>Tentar novamente</button>
      ) : <>
        <div className="overflow-x-auto rounded-xl border border-slate-700">
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-950 text-slate-400"><tr>
              <th className="p-3">Conta</th><th className="p-3">Papel</th><th className="p-3">Estado</th><th className="p-3">Tentativas</th><th className="p-3">Acções</th>
            </tr></thead>
            <tbody>{data.accounts.map(account => <tr key={account.id} className="border-t border-slate-800">
              <td className="p-3"><span className="font-semibold">{account.username}</span><span className="block text-xs text-slate-400">{account.displayName}</span></td>
              <td className="p-3">{account.role}</td>
              <td className="p-3">{account.active ? 'Activa' : 'Inactiva'}{account.lockedUntil && new Date(account.lockedUntil) > new Date() && <span className="block text-rose-400">Bloqueada</span>}</td>
              <td className="p-3">{account.failedAttempts}</td>
              <td className="p-3"><div className="flex flex-wrap gap-2 min-w-44">
                <button disabled={busy} onClick={() => select(account, 'edit')} aria-label={`Editar ${account.username}`} className="rounded border border-slate-600 px-2 py-1">Editar</button>
                <button disabled={busy} onClick={() => select(account, 'revoke')} aria-label={`Revogar sessões de ${account.username}`} className="rounded border border-slate-600 px-2 py-1">Revogar sessões</button>
                {(account.failedAttempts > 0 || account.lockedUntil) && <button disabled={busy} onClick={() => select(account, 'unlock')} aria-label={`Desbloquear ${account.username}`} className="rounded border border-slate-600 px-2 py-1">Desbloquear</button>}
              </div></td>
            </tr>)}</tbody>
          </table>
        </div>
        {data.accounts.length === 0 && <p className="p-3 text-sm">Nenhuma conta nesta página.</p>}
        <div className="flex items-center justify-between gap-2 my-4 text-sm">
          <button disabled={loading || busy || data.page === 0} onClick={() => { setSelected(null); void load(data.page - 1); }} className="disabled:opacity-40">Anterior</button>
          <span>Página {data.page + 1} de {Math.max(1, data.totalPages)} · {data.totalElements} contas</span>
          <button disabled={loading || busy || data.page + 1 >= data.totalPages} onClick={() => { setSelected(null); void load(data.page + 1); }} className="disabled:opacity-40">Seguinte</button>
        </div>
      </>}
      {selected && <form onSubmit={submit} className="border-t border-slate-700 pt-4 space-y-3">
        <h3 className="font-semibold">{action === 'edit' ? 'Editar' : action === 'revoke' ? 'Revogar sessões de' : 'Desbloquear'} {selected.username}</h3>
        {action === 'edit' && <div className="flex flex-wrap items-center gap-4">
          <label className="text-sm">Papel <select value={role} onChange={e => setRole(e.target.value as AdminAccount['role'])} disabled={busy} className="ml-2 rounded bg-slate-950 border border-slate-600 p-2"><option value="ADMIN">ADMIN</option><option value="OPERATOR">OPERATOR</option></select></label>
          <label className="text-sm flex items-center gap-2"><input type="checkbox" checked={active} onChange={e => setActive(e.target.checked)} disabled={busy} />Conta activa</label>
        </div>}
        <label className="block text-sm">Motivo (sem passwords nem dados de clientes)
          <textarea required minLength={10} maxLength={500} value={reason} onChange={e => setReason(e.target.value)} disabled={busy} className="mt-1 block w-full rounded-lg border border-slate-600 bg-slate-950 p-2" />
        </label>
        <label className="block text-sm">A sua password actual
          <input required type="password" autoComplete="current-password" maxLength={72} value={password} onChange={e => setPassword(e.target.value)} disabled={busy} className="mt-1 block w-full rounded-lg border border-slate-600 bg-slate-950 p-2" />
        </label>
        {selected.username === user?.username && <p className="text-sm text-amber-300">Esta operação termina a sua sessão.</p>}
        <div className="flex justify-end gap-3">
          <button type="button" disabled={busy} onClick={() => { setSelected(null); setPassword(''); }} className="rounded-lg border border-slate-600 px-3 py-2">Cancelar alteração</button>
          <button type="submit" disabled={busy} className="rounded-lg bg-amber-500 text-slate-950 font-semibold px-3 py-2 disabled:opacity-50">{busy ? 'A confirmar…' : 'Confirmar operação'}</button>
        </div>
      </form>}
    </dialog>
  );
}
