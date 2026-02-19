import { useState, useCallback } from "react";
import { apiClient } from "../api/client";
import { useAuthContext } from "../contexts/AuthContext";
import { useErrorContext } from "../contexts/ErrorContext";

interface UseAuthReturn {
  loading: boolean;
  handleLogin: (userId: string, password: string) => Promise<boolean>;
  handleLogout: () => void;
}

export function useAuth(): UseAuthReturn {
  const [loading, setLoading] = useState(false);
  const { login, logout } = useAuthContext();
  const { setError, clearError } = useErrorContext();

  const handleLogin = useCallback(
    async (userId: string, password: string): Promise<boolean> => {
      setLoading(true);
      clearError();
      try {
        const response = await apiClient.login(userId, password);
        login(response.data.userId, response.data.token);
        return true;
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "Authentication failed";
        setError({ code: "AUTH_ERR", message });
        return false;
      } finally {
        setLoading(false);
      }
    },
    [login, setError, clearError]
  );

  const handleLogout = useCallback(() => {
    logout();
    clearError();
  }, [logout, clearError]);

  return { loading, handleLogin, handleLogout };
}
