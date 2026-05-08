import { cva } from 'class-variance-authority'
import { AlertCircle, AlertTriangle, Info, RefreshCw, X } from 'lucide-react'
import type { AppError, ErrorSeverity } from '@/types/errors'
import { cn } from '@/lib/utils'

const errorDisplayVariants = cva(
  'relative rounded-default border-l-4 p-leaf-16 font-sans',
  {
    variants: {
      severity: {
        error: 'border-error-red-100 bg-error-red-20 text-neutral-100',
        warning: 'border-warning-yellow-100 bg-warning-yellow-20 text-neutral-100',
        info: 'border-navy-100 bg-action-blue-20 text-neutral-100',
      },
    },
    defaultVariants: {
      severity: 'error',
    },
  },
)

const SEVERITY_ICONS: Record<ErrorSeverity, typeof AlertCircle> = {
  error: AlertCircle,
  warning: AlertTriangle,
  info: Info,
}

const SEVERITY_LABELS: Record<ErrorSeverity, string> = {
  error: 'Error',
  warning: 'Warning',
  info: 'Information',
}

interface ErrorDisplayProps {
  error: AppError
  onDismiss?: () => void
  onRetry?: () => void
  className?: string
}

export function ErrorDisplay({ error, onDismiss, onRetry, className }: ErrorDisplayProps) {
  const Icon = SEVERITY_ICONS[error.severity]
  const label = SEVERITY_LABELS[error.severity]

  return (
    <div
      className={cn(errorDisplayVariants({ severity: error.severity }), className)}
      role="alert"
      aria-live="assertive"
    >
      <div className="flex items-start gap-3">
        <Icon
          className="mt-0.5 shrink-0"
          size={20}
          aria-hidden="true"
        />

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="font-bold text-sm uppercase tracking-wide">
              {label}
            </span>
            {error.code && (
              <code className="rounded-sm bg-neutral-05 px-1.5 py-0.5 text-xs font-mono text-neutral-80">
                {error.code}
              </code>
            )}
          </div>

          <p className="text-sm leading-relaxed">{error.message}</p>

          {error.details && (
            <details className="mt-2">
              <summary className="cursor-pointer text-xs text-neutral-80 hover:text-neutral-100">
                Show details
              </summary>
              <pre className="mt-1 overflow-x-auto whitespace-pre-wrap rounded-sm bg-neutral-05 p-2 text-xs text-neutral-80 font-mono">
                {error.details}
              </pre>
            </details>
          )}
        </div>

        <div className="flex items-center gap-1 shrink-0">
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="rounded-default p-1.5 text-neutral-80 hover:bg-neutral-10 hover:text-neutral-100 transition-colors"
              aria-label="Retry"
            >
              <RefreshCw size={16} />
            </button>
          )}
          {onDismiss && (
            <button
              type="button"
              onClick={onDismiss}
              className="rounded-default p-1.5 text-neutral-80 hover:bg-neutral-10 hover:text-neutral-100 transition-colors"
              aria-label="Dismiss"
            >
              <X size={16} />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
