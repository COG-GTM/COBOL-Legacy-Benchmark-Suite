import { useCallback, useState, type ReactNode } from 'react';
import ToastNotification, { ToastContainer } from '../components/errors/ToastNotification';
import { ToastContext } from './toastContextDef';

interface Toast {
  id: string;
  message: string;
  severity: 'success' | 'info' | 'warning' | 'error';
  duration: number;
}

let toastCounter = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    ({ message, severity, duration = 5000 }: { message: string; severity: Toast['severity']; duration?: number }) => {
      const id = `toast-${++toastCounter}`;
      setToasts((prev) => [...prev, { id, message, severity, duration }]);
    },
    [],
  );

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <ToastContainer>
        {toasts.map((t) => (
          <ToastNotification
            key={t.id}
            id={t.id}
            message={t.message}
            severity={t.severity}
            duration={t.duration}
            onDismiss={dismiss}
          />
        ))}
      </ToastContainer>
    </ToastContext.Provider>
  );
}
