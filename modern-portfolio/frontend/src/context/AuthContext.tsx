import { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { api } from '../lib/api';

interface User {
  id: string;
  username: string;
  email: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('token');
  }, []);

  // Restore user metadata on page reload when token exists
  useEffect(() => {
    if (token && !user) {
      (api.getMe() as Promise<{ data: User }>)
        .then((result) => setUser(result.data))
        .catch(() => logout());
    }
  }, [token, user, logout]);

  const login = useCallback(async (username: string, password: string) => {
    const result = await api.login({ username, password }) as { data: { token: string; user: User } };
    setToken(result.data.token);
    setUser(result.data.user);
    localStorage.setItem('token', result.data.token);
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
