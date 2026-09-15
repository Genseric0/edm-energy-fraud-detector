import { apiClient } from './client';
import { LoginResponse } from '../types/energy';

export const authApi = {
  login: (username: string, password: string) =>
    apiClient.post<LoginResponse>('/api/auth/login', { username, password }, /* skipAuth */ true),

  me: () => apiClient.get<LoginResponse>('/api/auth/me'),
};
