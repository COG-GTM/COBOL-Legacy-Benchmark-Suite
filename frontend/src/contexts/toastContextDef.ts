import { createContext } from 'react';

export interface ToastContextValue {
  showToast: (opts: { message: string; severity: 'success' | 'info' | 'warning' | 'error'; duration?: number }) => void;
}

export const ToastContext = createContext<ToastContextValue | null>(null);
