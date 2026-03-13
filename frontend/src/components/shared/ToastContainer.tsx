import { useToast } from '@/context/ToastContext';
import { X, CheckCircle, AlertCircle, AlertTriangle, Info } from 'lucide-react';

const iconMap = {
  success: CheckCircle,
  error: AlertCircle,
  warning: AlertTriangle,
  info: Info,
};

const colorMap = {
  success: 'border-[#4ADE80] bg-[#4ADE80]/10 text-[#4ADE80]',
  error: 'border-[#F87171] bg-[#F87171]/10 text-[#F87171]',
  warning: 'border-amber-400 bg-amber-400/10 text-amber-400',
  info: 'border-[#60A5FA] bg-[#60A5FA]/10 text-[#60A5FA]',
};

export function ToastContainer() {
  const { toasts, removeToast } = useToast();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2" role="status" aria-live="polite">
      {toasts.map((toast) => {
        const Icon = iconMap[toast.type];
        return (
          <div
            key={toast.id}
            className={`flex items-center gap-3 rounded-lg border px-4 py-3 shadow-lg backdrop-blur-sm ${colorMap[toast.type]}`}
          >
            <Icon className="h-5 w-5 shrink-0" aria-hidden="true" />
            <span className="text-sm font-medium text-white">{toast.message}</span>
            <button
              onClick={() => removeToast(toast.id)}
              className="ml-2 shrink-0 rounded-md p-1 hover:bg-white/10"
              aria-label="Dismiss notification"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        );
      })}
    </div>
  );
}
