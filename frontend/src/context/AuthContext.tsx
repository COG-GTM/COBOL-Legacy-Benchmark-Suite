import { useState, useCallback, type ReactNode } from 'react';
import type { User, AuthState } from '@/types';
import { AuthContext } from './authContextDef';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
  });

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const login = useCallback(async (username: string, _password: string): Promise<boolean> => {
    const mockUser: User = {
      username,
      role: username.toLowerCase().includes('admin') ? 'Admin' : 'Update',
    };
    setAuthState({ user: mockUser, isAuthenticated: true });
    return true;
  }, []);

  const logout = useCallback(() => {
    setAuthState({ user: null, isAuthenticated: false });
  }, []);

  return (
    <AuthContext.Provider value={{ ...authState, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
