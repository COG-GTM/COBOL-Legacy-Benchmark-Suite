import type { AppError } from "@/types/portfolio";
import { ERROR_SEVERITY_LABELS } from "@/types/portfolio";
import { AlertTriangle, AlertCircle, Info } from "lucide-react";

interface ErrorDisplayProps {
  error: AppError;
}

const SEVERITY_STYLES: Record<string, { bg: string; border: string; icon: string }> = {
  F: { bg: "bg-red-50", border: "border-red-400", icon: "text-red-600" },
  W: { bg: "bg-amber-50", border: "border-amber-400", icon: "text-amber-600" },
  I: { bg: "bg-blue-50", border: "border-blue-400", icon: "text-blue-600" },
};

const SEVERITY_ICONS = {
  F: AlertCircle,
  W: AlertTriangle,
  I: Info,
} as const;

export default function ErrorDisplay({ error }: ErrorDisplayProps) {
  const styles = SEVERITY_STYLES[error.severity] ?? SEVERITY_STYLES.I;
  const Icon = SEVERITY_ICONS[error.severity] ?? Info;

  return (
    <div
      className={`rounded-lg border-l-4 ${styles.border} ${styles.bg} p-4`}
      role="alert"
    >
      <div className="flex items-start gap-3">
        <Icon className={`h-5 w-5 mt-0.5 ${styles.icon}`} />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-sm font-semibold text-gray-900">
              {ERROR_SEVERITY_LABELS[error.severity]}
            </span>
            <code className="rounded bg-white/60 px-1.5 py-0.5 text-xs font-mono text-gray-700">
              {error.program}
            </code>
          </div>
          <p className="text-sm text-gray-800">{error.message}</p>
          {error.traceId && (
            <p className="mt-2 text-xs text-gray-500">
              Trace: <span className="font-mono">{error.traceId}</span>
              {error.timestamp && <> &middot; {error.timestamp}</>}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
