import { useEffect, useState } from 'react';
import { X, AlertTriangle, XCircle, AlertOctagon, Flame } from 'lucide-react';
import type { AppError, ErrorSeverity } from '@/data/types';
import { useErrors } from '@/context/ErrorContext';
import { SEVERITY_COLORS } from '@/data/errorConstants';

const SEVERITY_ICONS: Record<ErrorSeverity, typeof AlertTriangle> = {
  warning: AlertTriangle,
  error: XCircle,
  severe: AlertOctagon,
  critical: Flame,
};

function Toast({ error, onDismiss }: { error: AppError; onDismiss: (id: string) => void }) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const frame = requestAnimationFrame(() => setVisible(true));
    return () => cancelAnimationFrame(frame);
  }, []);

  const handleDismiss = () => {
    setVisible(false);
    setTimeout(() => onDismiss(error.id), 200);
  };

  const Icon = SEVERITY_ICONS[error.severity];
  const colors = SEVERITY_COLORS[error.severity];

  return (
    <div
      className={`flex items-start gap-3 w-80 p-4 rounded-lg border shadow-lg transition-all duration-200 ${colors.bg} ${colors.border} ${
        visible ? 'opacity-100 translate-x-0' : 'opacity-0 translate-x-4'
      }`}
    >
      <Icon className={`w-5 h-5 shrink-0 mt-0.5 ${colors.text}`} />
      <div className="flex-1 min-w-0">
        <p className={`text-xs font-semibold uppercase tracking-wide ${colors.text}`}>
          {error.code}
        </p>
        <p className={`text-sm mt-0.5 ${colors.text}`}>{error.message}</p>
      </div>
      <button
        onClick={handleDismiss}
        className={`shrink-0 p-0.5 rounded hover:bg-black/5 ${colors.text}`}
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}

export function ErrorToast() {
  const { errors, dismissError } = useErrors();

  if (errors.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2">
      {errors.map((error) => (
        <Toast key={error.id} error={error} onDismiss={dismissError} />
      ))}
    </div>
  );
}
