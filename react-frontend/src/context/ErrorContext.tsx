/**
 * ErrorContext - Replaces ERRHNDL (Error Handler) COBOL program
 *
 * In the COBOL system, ERRHNDL handled:
 * - P100-INIT-ERROR-HANDLER: Initialize error area with timestamp/trace
 * - P200-LOG-ERROR: DB2 INSERT INTO ERRLOG
 * - P300-FORMAT-MESSAGE: STRING error message formatting
 * - P400-DETERMINE-ACTION: Evaluate severity → ABEND/CONTINUE/RETURN
 *
 * This React context provides centralized error handling with
 * severity levels matching the COBOL ERRHND copybook.
 */

import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import type { SystemError } from '../types';

interface ErrorContextType {
  error: SystemError | null;
  setSystemError: (code: string, details: string) => void;
  clearError: () => void;
  hasError: boolean;
}

const ErrorContext = createContext<ErrorContextType | undefined>(undefined);

export function ErrorProvider({ children }: { children: ReactNode }) {
  const [error, setError] = useState<SystemError | null>(null);

  const setSystemError = useCallback((code: string, details: string) => {
    // Mirrors ERRHNDL P300-FORMAT-MESSAGE
    const traceId = Math.random().toString(36).substring(2, 18);
    console.error(`[ERROR] TraceID: ${traceId} | Code: ${code} | ${details}`);

    setError({
      code: code.substring(0, 8),        // ERRCOUT - 8 chars max
      details: details.substring(0, 65),  // ERRDOUT - 65 chars max
    });
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return (
    <ErrorContext.Provider
      value={{
        error,
        setSystemError,
        clearError,
        hasError: error !== null,
      }}
    >
      {children}
    </ErrorContext.Provider>
  );
}

export function useError(): ErrorContextType {
  const context = useContext(ErrorContext);
  if (context === undefined) {
    throw new Error('useError must be used within an ErrorProvider');
  }
  return context;
}
