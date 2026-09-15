/**
 * Cliente HTTP central para comunicação com o backend Spring Boot.
 *
 * Todos os pedidos passam por aqui para garantir que:
 * - o token JWT é sempre incluído quando existe sessão ativa
 * - erros de rede/autenticação são tratados de forma consistente
 * - a base URL fica num único sítio (o proxy do Vite trata do resto em dev)
 */

const TOKEN_STORAGE_KEY = 'gridaudit_jwt_token';

export function getStoredToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setStoredToken(token: string): void {
  try {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
  } catch {
    // Ambiente sem localStorage disponível (raro); a sessão simplesmente não persiste.
  }
}

export function clearStoredToken(): void {
  try {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  } catch {
    // Nada a fazer.
  }
}

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  /** Se true, não anexa o cabeçalho Authorization (usado apenas no login). */
  skipAuth?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, skipAuth = false } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (!skipAuth) {
    const token = getStoredToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (networkError) {
    throw new ApiError(
      'Não foi possível contactar o servidor. Verifique se o backend Spring Boot está a correr em http://localhost:8080.',
      0
    );
  }

  if (response.status === 401 || response.status === 403) {
    // Token inválido/expirado ou sem permissão: limpa a sessão local.
    clearStoredToken();
    throw new ApiError('Sessão expirada ou sem permissão. Faça login novamente.', response.status);
  }

  if (!response.ok) {
    let message = `Erro ${response.status} ao comunicar com o servidor.`;
    try {
      const errorBody = await response.json();
      if (errorBody?.message) message = errorBody.message;
    } catch {
      // Corpo de erro não era JSON; mantém a mensagem genérica.
    }
    throw new ApiError(message, response.status);
  }

  // Pedidos sem corpo de resposta (ex.: 204 No Content)
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown, skipAuth = false) =>
    request<T>(path, { method: 'POST', body, skipAuth }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
