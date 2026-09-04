import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { Role } from '../types';
import { api } from '../services/api';

// ── Tipos ─────────────────────────────────────────────────────────────────────

interface AuthUser {
  role: Role;
  name: string;
}

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (cpf: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

// ── Contexto ──────────────────────────────────────────────────────────────────

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// ── Provider ──────────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);

  useEffect(() => {
    // Testa a sessão tentando acessar uma rota protegida da API.
    // Se o cookie jwt_token estiver presente, o navegador o enviará automaticamente
    // e o backend validará a autenticação. Em caso de 401, limpa o estado.
    api.get('/API/PaymentEmpenho', { params: { page: 0, size: 1 } }).then(
      () => {
        setIsAuthenticated(true);
      },
      () => {
        setIsAuthenticated(false);
        setUser(null);
      }
    );
  }, []);

  const login = async (cpf: string, password: string) => {
    try {
      const res = await api.post<{ id: number; name: string; cpf: string; role: Role }>('/API/User/login', { cpf, password });
      setIsAuthenticated(true);
      setUser({ role: res.data.role, name: res.data.name });
    } catch (error) {
      setIsAuthenticated(false);
      setUser(null);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await api.post('/API/User/logout');
    } catch (error) {
      console.error('Erro ao fazer logout', error);
    } finally {
      setIsAuthenticated(false);
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// ── Hook ──────────────────────────────────────────────────────────────────────

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider');
  return ctx;
}
