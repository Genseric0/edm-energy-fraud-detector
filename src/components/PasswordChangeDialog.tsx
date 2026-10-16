import React, { useEffect, useRef, useState } from 'react';
import { authApi } from '../api/authApi';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';

export function PasswordChangeDialog({ onClose }: { onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const { logout } = useAuth();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => { dialog.current?.showModal(); }, []);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (busy) return;
    setError('');
    if (next !== confirmation) { setError('A confirmação não coincide.'); return; }
    if (next.trim().length === 0 || next.length < 12 || new TextEncoder().encode(next).length > 72) {
      setError('Use pelo menos 12 caracteres e no máximo 72 bytes UTF-8.'); return;
    }
    setBusy(true);
    try {
      await authApi.changePassword(current, next);
      setCurrent(''); setNext(''); setConfirmation('');
      logout();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Não foi possível alterar a password. Tente novamente.');
      setCurrent('');
    } finally { setBusy(false); }
  }

  return (
    <dialog ref={dialog} onCancel={onClose} aria-labelledby="password-title"
      className="m-auto w-11/12 max-w-md max-h-[90dvh] overflow-y-auto rounded-2xl border border-slate-700 bg-slate-900 p-6 text-slate-100 backdrop:bg-slate-950/80">
      <form onSubmit={submit} className="space-y-4">
        <h2 id="password-title" className="text-lg font-bold">Alterar password</h2>
        <p className="text-sm text-slate-400">Use 12 ou mais caracteres, até 72 bytes UTF-8. Ao guardar, todas as sessões desta conta terminam. Entre novamente com a nova password.</p>
        <label className="block text-sm">Password actual
          <input autoFocus required type="password" autoComplete="current-password" maxLength={72}
            value={current} onChange={e => setCurrent(e.target.value)} disabled={busy}
            className="mt-1 block w-full rounded-lg bg-slate-950 border border-slate-700 p-2" />
        </label>
        <label className="block text-sm">Nova password
          <input required type="password" autoComplete="new-password" minLength={12} maxLength={72}
            value={next} onChange={e => setNext(e.target.value)} disabled={busy}
            className="mt-1 block w-full rounded-lg bg-slate-950 border border-slate-700 p-2" />
        </label>
        <label className="block text-sm">Confirmar nova password
          <input required type="password" autoComplete="new-password" minLength={12} maxLength={72}
            value={confirmation} onChange={e => setConfirmation(e.target.value)} disabled={busy}
            className="mt-1 block w-full rounded-lg bg-slate-950 border border-slate-700 p-2" />
        </label>
        {error && <p role="alert" className="text-sm text-rose-400">{error}</p>}
        <div className="flex justify-end gap-3">
          <button type="button" onClick={onClose} disabled={busy} className="rounded-lg border border-slate-700 px-3 py-2">Cancelar</button>
          <button type="submit" disabled={busy} className="rounded-lg bg-amber-500 px-3 py-2 font-semibold text-slate-950 disabled:opacity-50">{busy ? 'A guardar…' : 'Guardar password'}</button>
        </div>
      </form>
    </dialog>
  );
}
