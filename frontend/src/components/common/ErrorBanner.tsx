import { X } from 'lucide-react';

interface ErrorBannerProps {
  message: string;
  severity: 'error' | 'warning' | 'info' | 'success';
  onDismiss: () => void;
}

const severityStyles: Record<ErrorBannerProps['severity'], string> = {
  error: 'bg-red-50 border-red-200 text-red-800',
  warning: 'bg-yellow-50 border-yellow-200 text-yellow-800',
  info: 'bg-blue-50 border-blue-200 text-blue-800',
  success: 'bg-green-50 border-green-200 text-green-800',
};

const severityIcons: Record<ErrorBannerProps['severity'], string> = {
  error: 'Error',
  warning: 'Warning',
  info: 'Info',
  success: 'Success',
};

export default function ErrorBanner({ message, severity, onDismiss }: ErrorBannerProps) {
  return (
    <div
      className={`flex items-center justify-between px-4 py-3 border rounded-lg mb-4 ${severityStyles[severity]}`}
      role="alert"
    >
      <div className="flex items-center gap-2">
        <span className="font-semibold text-sm">{severityIcons[severity]}:</span>
        <span className="text-sm">{message}</span>
      </div>
      <button
        onClick={onDismiss}
        className="p-1 hover:opacity-70 transition-opacity rounded"
        aria-label="Dismiss"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}
