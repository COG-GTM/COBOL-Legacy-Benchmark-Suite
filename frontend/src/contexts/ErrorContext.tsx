import { createContext, useContext, useState, useCallback, ReactNode } from "react";

interface ErrorState {
  code?: string;
  message: string;
  traceId?: string;
}

interface ErrorContextType {
  error: ErrorState | null;
  setError: (error: ErrorState) => void;
  clearError: () => void;
}

const ErrorContext = createContext<ErrorContextType | undefined>(undefined);

export function ErrorProvider({ children }: { children: ReactNode }) {
  const [error, setErrorState] = useState<ErrorState | null>(null);

  const setError = useCallback((err: ErrorState) => {
    setErrorState(err);
  }, []);

  const clearError = useCallback(() => {
    setErrorState(null);
  }, []);

  return (
    <ErrorContext.Provider value={{ error, setError, clearError }}>
      {children}
    </ErrorContext.Provider>
  );
}

export function useErrorContext(): ErrorContextType {
  const context = useContext(ErrorContext);
  if (!context) {
    throw new Error("useErrorContext must be used within an ErrorProvider");
  }
  return context;
}
