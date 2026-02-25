/**
 * AuthContext — replaces SECMGR.cbl authorization checks.
 * Provides simple auth state tracking (login/logout) and
 * a ProtectedRoute component to guard authenticated routes.
 */

import {
  createContext,
  useContext,
  useState,
  useCallback,
  type ReactNode,
} from "react";
import { Navigate, useLocation } from "react-router-dom";

interface AuthState {
  isAuthenticated: boolean;
  userId: string | null;
  login: (userId: string, password: string) => Promise<boolean>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

/**
 * Mock credential validation (mirrors AUTHFILE DB2 table from SECMGR).
 * In production this would call a real authentication API.
 * For the demo, any non-empty user ID and password are accepted.
 */
function validateCredentials(id: string, pass: string): boolean {
  return id.trim().length > 0 && pass.trim().length > 0;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userId, setUserId] = useState<string | null>(null);

  const login = useCallback(
    async (id: string, password: string): Promise<boolean> => {
      // Simulate SECMGR P100-VALIDATE-USER + P200-CHECK-AUTH
      return new Promise((resolve) => {
        setTimeout(() => {
          if (validateCredentials(id, password)) {
            setIsAuthenticated(true);
            setUserId(id);
            resolve(true);
          } else {
            resolve(false);
          }
        }, 200);
      });
    },
    []
  );

  const logout = useCallback(() => {
    setIsAuthenticated(false);
    setUserId(null);
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, userId, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}

/** Route guard — redirects unauthenticated users to /login */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
