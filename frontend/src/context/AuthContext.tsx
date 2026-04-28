import { useState, useEffect, useCallback, type ReactNode } from 'react';
import { AuthContext } from './authContextDef';
import type { UserRole } from './authContextDef';

const SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes

interface AuthState {
  isAuthenticated: boolean;
  userId: string;
  role: UserRole;
  loginTime: number | null;
}

const LOGGED_OUT: AuthState = { isAuthenticated: false, userId: '', role: 'read-only', loginTime: null };

export type { UserRole };

function loadAuth(): AuthState {
  const stored = sessionStorage.getItem('auth');
  if (stored) {
    const parsed = JSON.parse(stored) as AuthState;
    if (parsed.loginTime && Date.now() - parsed.loginTime < SESSION_TIMEOUT_MS) {
      return parsed;
    }
    sessionStorage.removeItem('auth');
  }
  return LOGGED_OUT;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(loadAuth);

  const logout = useCallback(() => {
    sessionStorage.removeItem('auth');
    setAuth(LOGGED_OUT);
  }, []);

  const login = useCallback((userId: string, role: UserRole = 'portfolio-manager') => {
    const newAuth: AuthState = {
      isAuthenticated: true,
      userId: userId.toUpperCase(),
      role,
      loginTime: Date.now(),
    };
    sessionStorage.setItem('auth', JSON.stringify(newAuth));
    setAuth(newAuth);
  }, []);

  useEffect(() => {
    if (!auth.isAuthenticated || !auth.loginTime) return;
    const remaining = SESSION_TIMEOUT_MS - (Date.now() - auth.loginTime);
    if (remaining <= 0) return;
    const timer = setTimeout(() => {
      sessionStorage.removeItem('auth');
      setAuth(LOGGED_OUT);
    }, remaining);
    return () => clearTimeout(timer);
  }, [auth.isAuthenticated, auth.loginTime]);

  return (
    <AuthContext.Provider value={{ ...auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
