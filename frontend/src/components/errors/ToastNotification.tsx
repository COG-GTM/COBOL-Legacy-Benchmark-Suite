import { useEffect, type CSSProperties } from 'react';

export interface ToastNotificationProps {
  id: string;
  message: string;
  severity: 'success' | 'info' | 'warning' | 'error';
  duration?: number;
  onDismiss: (id: string) => void;
}

const SEVERITY_COLORS: Record<ToastNotificationProps['severity'], string> = {
  success: '#2e7d32',
  info: '#0288d1',
  warning: '#ed6c02',
  error: '#d32f2f',
};

const containerStyle: CSSProperties = {
  position: 'fixed',
  top: 16,
  right: 16,
  zIndex: 9999,
};

const toastStyle = (severity: ToastNotificationProps['severity']): CSSProperties => ({
  backgroundColor: SEVERITY_COLORS[severity],
  color: '#fff',
  padding: '12px 24px',
  borderRadius: 6,
  marginBottom: 8,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  minWidth: 280,
  boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
  gap: 12,
});

const dismissBtnStyle: CSSProperties = {
  background: 'none',
  border: 'none',
  color: '#fff',
  fontSize: '1.1rem',
  cursor: 'pointer',
  padding: 0,
  lineHeight: 1,
};

/**
 * A single toast notification.
 * Auto-dismisses after `duration` ms (default 5 000).
 */
export default function ToastNotification({
  id,
  message,
  severity,
  duration = 5000,
  onDismiss,
}: ToastNotificationProps) {
  useEffect(() => {
    const timer = setTimeout(() => onDismiss(id), duration);
    return () => clearTimeout(timer);
  }, [id, duration, onDismiss]);

  return (
    <div role="status" data-testid="toast" style={toastStyle(severity)}>
      <span>{message}</span>
      <button
        aria-label="Dismiss"
        data-testid="toast-dismiss"
        onClick={() => onDismiss(id)}
        style={dismissBtnStyle}
      >
        &times;
      </button>
    </div>
  );
}

/** Container that positions toasts in the top-right viewport corner. */
export function ToastContainer({ children }: { children: React.ReactNode }) {
  return <div style={containerStyle}>{children}</div>;
}
