import { apiClient } from './client';

export interface AdminAccount {
  id: number; username: string; displayName: string; role: 'ADMIN' | 'OPERATOR';
  active: boolean; failedAttempts: number; lockedUntil: string | null; version: number;
}
export interface AccountPage { accounts: AdminAccount[]; page: number; totalPages: number; totalElements: number }
export interface AccountAction { expectedVersion: number; reason: string; currentPassword: string }
export const adminAccountsApi = {
  list: (page = 0) => apiClient.get<AccountPage>(`/api/admin/accounts?page=${page}&size=20`),
  update: (id: number, input: AccountAction & Pick<AdminAccount, 'role' | 'active'>) =>
    apiClient.put<AdminAccount>(`/api/admin/accounts/${id}`, input),
  revoke: (id: number, input: AccountAction) => apiClient.post<AdminAccount>(`/api/admin/accounts/${id}/revoke-sessions`, input),
  unlock: (id: number, input: AccountAction) => apiClient.post<AdminAccount>(`/api/admin/accounts/${id}/unlock`, input),
};
