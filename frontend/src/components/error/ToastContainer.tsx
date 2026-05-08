import { useToast } from '@/hooks/useToast'
import { ErrorDisplay } from './ErrorDisplay'
import { cn } from '@/lib/utils'

interface ToastContainerProps {
  className?: string
}

export function ToastContainer({ className }: ToastContainerProps) {
  const { toasts, dismiss } = useToast()

  if (toasts.length === 0) return null

  return (
    <div
      className={cn(
        'fixed bottom-4 right-4 z-50 flex flex-col gap-2 w-full max-w-md',
        className,
      )}
      aria-live="polite"
      aria-label="Notifications"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          className="animate-in slide-in-from-right duration-300 shadow-default"
          role="status"
        >
          <ErrorDisplay
            error={t.error}
            onDismiss={() => dismiss(t.id)}
          />
        </div>
      ))}
    </div>
  )
}
