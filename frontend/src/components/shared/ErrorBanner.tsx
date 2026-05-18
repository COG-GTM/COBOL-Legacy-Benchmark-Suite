import { AlertCircle, AlertTriangle, X } from 'lucide-react';
import { cn } from '@/utils/cn';

interface ErrorBannerProps {
  code?: string;
  message: string;
  severity?: 'Error' | 'Warning';
  onDismiss?: () => void;
  className?: string;
}

export function ErrorBanner({ code, message, severity = 'Error', onDismiss, className }: ErrorBannerProps) {
  const isError = severity === 'Error';

  return (
    <div
      className={cn(
        'flex items-start gap-3 rounded-lg border p-4',
        isError ? 'border-danger/30 bg-danger/5 text-danger' : 'border-warning/30 bg-warning/5 text-warning',
        className
      )}
      role="alert"
    >
      {isError ? <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" /> : <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />}
      <div className="flex-1 min-w-0">
        {code && <span className="font-mono font-semibold text-sm mr-2">{code}</span>}
        <span className="text-sm">{message}</span>
      </div>
      {onDismiss && (
        <button onClick={onDismiss} className="shrink-0 hover:opacity-70" aria-label="Dismiss">
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
}
