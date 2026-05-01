import { useState, useCallback } from 'react';
import type { AuthUser } from '../types';

export function useAuth() {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('token')
  );

  const loginUser = useCallback((authUser: AuthUser, authToken: string) => {
    localStorage.setItem('user', JSON.stringify(authUser));
    localStorage.setItem('token', authToken);
    setUser(authUser);
    setToken(authToken);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    setUser(null);
    setToken(null);
  }, []);

  return { user, token, isAuthenticated: !!token, loginUser, logout };
}
