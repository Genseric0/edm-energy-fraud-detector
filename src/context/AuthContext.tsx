import React, { createContext, useContext, useState, useEffect } from 'react';
import { AuthUser } from '../types/energy';
import { authApi } from '../api/authApi';
import { ApiError, getStoredToken, setStoredToken, clearStoredToken } from '../api/client';

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<{ success: boolean; message: string }>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);

  // Ao carregar a aplicação com um token guardado, confirma junto do backend
  // que a sessão ainda é válida (o servidor pode ter reiniciado, ou o token expirado).
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    authApi
      .me()
      .then((response) => {
        if (response.authenticated && response.username) {
          const authUser: AuthUser = {
            username: response.username,
            name: response.name ?? response.username,
            role: response.role ?? 'ROLE_OPERATOR',
            authorities: response.authorities ?? [],
          };
          setUser(authUser);
        } else {
          clearStoredToken();
          setUser(null);
        }
      })
      .catch(() => {
        // Token inválido/expirado ou backend indisponível: limpa a sessão local.
        clearStoredToken();
        setUser(null);
      })
      .finally(() => setIsLoading(false));
  }, []);

  useEffect(() => {
    const expire = () => setUser(null);
    window.addEventListener('gridaudit-session-expired', expire);
    return () => window.removeEventListener('gridaudit-session-expired', expire);
  }, []);

  const login = async (username: string, password: string): Promise<{ success: boolean; message: string }> => {
    try {
      const response = await authApi.login(username.trim(), password);

      if (response.authenticated && response.token && response.username) {
        setStoredToken(response.token);

        const authUser: AuthUser = {
          username: response.username,
          name: response.name ?? response.username,
          role: response.role ?? 'ROLE_OPERATOR',
          authorities: response.authorities ?? [],
        };
        setUser(authUser);

        const profileType = authUser.role === 'ROLE_ADMIN' ? 'Administrador(a)' : 'Operador(a)';
        return {
          success: true,
          message: `Bem-vindo(a), ${authUser.name}! Autenticado(a) com sucesso como ${profileType}.`,
        };
      }

      return {
        success: false,
        message: response.message ?? 'Credenciais inválidas.',
      };
    } catch (err) {
      if (err instanceof ApiError) {
        // Erro de rede (status 0): backend provavelmente não está a correr.
        if (err.status === 0 || err.status === 429) {
          return { success: false, message: err.message };
        }
        return { success: false, message: 'Credenciais inválidas. Verifique o usuário e a senha.' };
      }
      return { success: false, message: 'Erro inesperado ao autenticar. Tente novamente.' };
    }
  };

  const logout = () => {
    clearStoredToken();
    setUser(null);
  };

  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isAuthenticated = !!user;

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isAdmin, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser utilizado dentro de um AuthProvider');
  }
  return context;
}
