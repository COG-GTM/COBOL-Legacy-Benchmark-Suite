import { AlertCircle } from 'lucide-react'

import { cn } from '@/lib/utils'

import type { StatusMessage } from './shellContext'

/** Bottom message line — the web equivalent of MENMAP's ERRMSG field on row 23. */
export function StatusLine({ message }: { message: StatusMessage | null }) {
  const isError = message?.tone === 'error'
  return (
    <div
      role={isError ? 'alert' : 'status'}
      aria-live={isError ? 'assertive' : 'polite'}
      data-testid="status-line"
      className={cn(
        'flex min-h-10 items-center gap-2 border-t bg-card px-4 text-sm md:px-6',
        isError ? 'font-medium text-destructive' : 'text-muted-foreground',
      )}
    >
      {isError && <AlertCircle className="h-4 w-4 shrink-0" aria-hidden="true" />}
      <span className="truncate">{message?.text ?? 'Ready'}</span>
    </div>
  )
}
