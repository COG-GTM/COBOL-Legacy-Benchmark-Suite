/**
 * AuthContext - Replaces SECMGR (Security Manager) COBOL program
 *
 * In the COBOL system, SECMGR handled:
 * - P100-VALIDATE-USER: CICS ASSIGN USERID validation
 * - P200-CHECK-AUTH: DB2 AUTHFILE authorization lookup
 * - P300-LOG-ACCESS: DB2 AUDITLOG insert
 *
 * This React context replaces all three functions with a centralized
 * authentication and authorization state manager.
 */

import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { validateAuth } from '../api/portfolioApi';
import type { AuthState } from '../types';

interface AuthContextType {
  auth: AuthState;
  login: (userId: string) => Promise<boolean>;
  logout: () => void;
  error: string | null;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>({
    isAuthenticated: false,
    userId: '',
    sessionActive: false,
  });
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const login = useCallback(async (userId: string): Promise<boolean> => {
    setIsLoading(true);
    setError(null);

    const response = await validateAuth(userId, 'INQONLN');

    if (response.success && response.data && response.data.responseCode === 0) {
      setAuth({
        isAuthenticated: true,
        userId: userId.toUpperCase().trim(),
        sessionActive: true,
      });
      setIsLoading(false);
      return true;
    } else {
      setError(response.error || 'Authentication failed');
      setAuth({
        isAuthenticated: false,
        userId: '',
        sessionActive: false,
      });
      setIsLoading(false);
      return false;
    }
  }, []);

  const logout = useCallback(() => {
    // Mirrors INQONLN: SET SESSION-TERMINATED TO TRUE
    setAuth({
      isAuthenticated: false,
      userId: '',
      sessionActive: false,
    });
    setError(null);
  }, []);

  return (
    <AuthContext.Provider value={{ auth, login, logout, error, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
