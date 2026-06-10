/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';

interface AuthState {
  isAuthenticated: boolean;
  userId: string | null;
}

interface AuthContextType extends AuthState {
  login: (userId: string, password: string) => boolean;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(() => {
    try {
      const stored = sessionStorage.getItem('auth');
      if (stored) {
        const parsed = JSON.parse(stored);
        if (typeof parsed.isAuthenticated === 'boolean' && (parsed.userId === null || typeof parsed.userId === 'string')) {
          return parsed as AuthState;
        }
      }
    } catch {
      sessionStorage.removeItem('auth');
    }
    return { isAuthenticated: false, userId: null };
  });

  const login = useCallback((userId: string, _password: string) => {
    if (userId.trim() && _password.trim()) {
      const newAuth = { isAuthenticated: true, userId: userId.toUpperCase() };
      setAuth(newAuth);
      sessionStorage.setItem('auth', JSON.stringify(newAuth));
      return true;
    }
    return false;
  }, []);

  const logout = useCallback(() => {
    setAuth({ isAuthenticated: false, userId: null });
    sessionStorage.removeItem('auth');
  }, []);

  return (
    <AuthContext.Provider value={{ ...auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
