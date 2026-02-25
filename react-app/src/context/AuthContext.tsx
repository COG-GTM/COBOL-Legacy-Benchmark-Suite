import { createContext, useContext, useState, useCallback, type ReactNode } from "react";

interface AuthState {
  isAuthenticated: boolean;
  userId: string;
  userName: string;
}

interface AuthContextType extends AuthState {
  login: (userId: string, password: string) => Promise<boolean>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * Replaces SECMGR.cbl authorization checks.
 * - SEC-VALIDATE: validates user credentials (login)
 * - SEC-AUTHORIZE: checks resource access (protected routes)
 * - SEC-AUDIT: logs access (console log for now)
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>({
    isAuthenticated: false,
    userId: "",
    userName: "",
  });

  const login = useCallback(async (userId: string, password: string): Promise<boolean> => {
    // Simulate SEC-VALIDATE (P100-VALIDATE-USER) — accept any non-empty credentials
    await new Promise((resolve) => setTimeout(resolve, 200));

    if (userId.trim() && password.trim()) {
      // SEC-AUDIT (P300-LOG-ACCESS)
      console.log(
        `[AUDIT] User login: ${userId} at ${new Date().toISOString()}`
      );

      setAuthState({
        isAuthenticated: true,
        userId: userId.toUpperCase().slice(0, 8), // PIC X(8)
        userName: userId,
      });
      return true;
    }
    return false;
  }, []);

  const logout = useCallback(() => {
    console.log(
      `[AUDIT] User logout: ${authState.userId} at ${new Date().toISOString()}`
    );
    setAuthState({
      isAuthenticated: false,
      userId: "",
      userName: "",
    });
  }, [authState.userId]);

  return (
    <AuthContext.Provider value={{ ...authState, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
