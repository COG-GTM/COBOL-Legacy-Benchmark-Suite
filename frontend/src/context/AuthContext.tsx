import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import type { AuthState, User } from '../types';
import usersData from '../mocks/users.json';

interface AuthContextType extends AuthState {
  login: (username: string, password: string) => boolean;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>({ user: null, isAuthenticated: false });

  const login = useCallback((username: string, password: string): boolean => {
    const found = (usersData as User[]).find(
      (u) => u.username === username && u.password === password,
    );
    if (found) {
      const { password: _, ...user } = found;
      void _;
      setAuth({ user, isAuthenticated: true });
      return true;
    }
    return false;
  }, []);

  const logout = useCallback(() => {
    setAuth({ user: null, isAuthenticated: false });
  }, []);

  return (
    <AuthContext.Provider value={{ ...auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
