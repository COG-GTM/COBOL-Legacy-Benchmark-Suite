/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import type { AppError } from '@/data/types';

interface ErrorContextType {
  errors: AppError[];
  addError: (error: Omit<AppError, 'id' | 'timestamp'>) => void;
  dismissError: (id: string) => void;
  clearErrors: () => void;
}

const ErrorContext = createContext<ErrorContextType | null>(null);

let nextId = 1;

export function ErrorProvider({ children }: { children: ReactNode }) {
  const [errors, setErrors] = useState<AppError[]>([]);
  const timersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  const dismissError = useCallback((id: string) => {
    setErrors((prev) => prev.filter((e) => e.id !== id));
    const timer = timersRef.current.get(id);
    if (timer) {
      clearTimeout(timer);
      timersRef.current.delete(id);
    }
  }, []);

  const addError = useCallback(
    (error: Omit<AppError, 'id' | 'timestamp'>) => {
      const id = `err-${nextId++}`;
      const newError: AppError = {
        ...error,
        id,
        timestamp: new Date().toISOString(),
      };
      setErrors((prev) => [...prev, newError]);

      if (newError.severity === 'warning') {
        const timer = setTimeout(() => {
          setErrors((prev) => prev.filter((e) => e.id !== id));
          timersRef.current.delete(id);
        }, 5000);
        timersRef.current.set(id, timer);
      }
    },
    [],
  );

  const clearErrors = useCallback(() => {
    timersRef.current.forEach((timer) => clearTimeout(timer));
    timersRef.current.clear();
    setErrors([]);
  }, []);

  useEffect(() => {
    const timers = timersRef.current;
    return () => {
      timers.forEach((timer) => clearTimeout(timer));
    };
  }, []);

  return (
    <ErrorContext.Provider value={{ errors, addError, dismissError, clearErrors }}>
      {children}
    </ErrorContext.Provider>
  );
}

export function useErrors(): ErrorContextType {
  const context = useContext(ErrorContext);
  if (!context) {
    throw new Error('useErrors must be used within an ErrorProvider');
  }
  return context;
}
