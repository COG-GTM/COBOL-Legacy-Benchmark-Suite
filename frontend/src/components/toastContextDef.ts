import { createContext } from 'react';

type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastContextType {
  addToast: (message: string, type?: ToastType) => void;
}

export const ToastContext = createContext<ToastContextType | null>(null);
