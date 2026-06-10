import { useState } from 'react';
import { X, ChevronDown, ChevronUp, AlertTriangle, XCircle, AlertOctagon, Flame } from 'lucide-react';
import type { AppError, ErrorSeverity } from '@/data/types';
import { CATEGORY_LABELS, SEVERITY_COLORS } from '@/data/errorConstants';

const SEVERITY_ICONS: Record<ErrorSeverity, typeof AlertTriangle> = {
  warning: AlertTriangle,
  error: XCircle,
  severe: AlertOctagon,
  critical: Flame,
};

interface ErrorBannerProps {
  error: AppError;
  onDismiss?: (id: string) => void;
}

export function ErrorBanner({ error, onDismiss }: ErrorBannerProps) {
  const [expanded, setExpanded] = useState(false);

  const Icon = SEVERITY_ICONS[error.severity];
  const colors = SEVERITY_COLORS[error.severity];

  return (
    <div className={`w-full rounded-lg border p-4 ${colors.bg} ${colors.border}`}>
      <div className="flex items-start gap-3">
        <Icon className={`w-5 h-5 shrink-0 mt-0.5 ${colors.text}`} />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${colors.bg} ${colors.text} ${colors.ring}`}
            >
              {CATEGORY_LABELS[error.category]}
            </span>
            <span className={`text-sm font-semibold ${colors.text}`}>{error.code}</span>
          </div>
          <p className={`text-sm mt-1 ${colors.text}`}>{error.message}</p>
          {error.details && (
            <button
              onClick={() => setExpanded((prev) => !prev)}
              className={`mt-1 flex items-center gap-1 text-xs font-medium ${colors.text} hover:underline`}
            >
              {expanded ? (
                <>
                  Hide details <ChevronUp className="w-3 h-3" />
                </>
              ) : (
                <>
                  Show details <ChevronDown className="w-3 h-3" />
                </>
              )}
            </button>
          )}
          {expanded && error.details && (
            <p className={`mt-2 text-xs font-mono whitespace-pre-wrap ${colors.text} opacity-80`}>
              {error.details}
            </p>
          )}
        </div>
        {(error.dismissible ?? true) && onDismiss && (
          <button
            onClick={() => onDismiss(error.id)}
            className={`shrink-0 p-0.5 rounded hover:bg-black/5 ${colors.text}`}
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  );
}
