import { useState } from 'react'
import { ErrorBoundary } from '@/components/error/ErrorBoundary'
import { ErrorDisplay } from '@/components/error/ErrorDisplay'
import { ToastContainer } from '@/components/error/ToastContainer'
import { useToast } from '@/hooks/useToast'
import { createAppError } from '@/types/errors'
import type { ErrorSeverity } from '@/types/errors'

function ErrorDemo() {
  const { toast: showToast } = useToast()
  const [showError, setShowError] = useState(false)
  const [showWarning, setShowWarning] = useState(false)
  const [showInfo, setShowInfo] = useState(false)

  const handleToast = (severity: ErrorSeverity) => {
    const messages: Record<ErrorSeverity, string> = {
      error: 'Database connection failed. Please try again.',
      warning: 'Session will expire in 5 minutes.',
      info: 'Portfolio data has been refreshed.',
    }
    showToast(messages[severity], severity)
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-2 text-3xl font-bold text-navy-100">
        Error Handling Components
      </h1>
      <p className="mb-8 text-sm text-neutral-80">
        COBOL ERRMAP → React Error Display, Boundary, and Toast system
      </p>

      <section className="mb-8">
        <h2 className="mb-4 text-xl font-medium text-neutral-100">Error Display</h2>
        <div className="flex flex-wrap gap-2 mb-4">
          <button
            type="button"
            onClick={() => setShowError(!showError)}
            className="rounded-pill bg-error-red-100 px-4 py-2 text-sm font-bold text-neutral-white hover:bg-error-red-180 transition-colors"
          >
            Toggle Error
          </button>
          <button
            type="button"
            onClick={() => setShowWarning(!showWarning)}
            className="rounded-pill bg-warning-yellow-100 px-4 py-2 text-sm font-bold text-neutral-100 hover:opacity-80 transition-colors"
          >
            Toggle Warning
          </button>
          <button
            type="button"
            onClick={() => setShowInfo(!showInfo)}
            className="rounded-pill bg-action-blue-100 px-4 py-2 text-sm font-bold text-neutral-white hover:bg-action-blue-140 transition-colors"
          >
            Toggle Info
          </button>
        </div>

        <div className="space-y-3">
          {showError && (
            <ErrorDisplay
              error={createAppError('E004', 'Database error: VSAM file access failure', 'error', {
                category: 'vsam',
                type: 'database',
                details: 'VSAM OPEN error on PORTMSTR file. Return code: 8. Severity: Fatal.',
              })}
              onDismiss={() => setShowError(false)}
              onRetry={() => setShowError(false)}
            />
          )}
          {showWarning && (
            <ErrorDisplay
              error={createAppError('E003', 'Invalid input data: Account number must be 10 digits', 'warning', {
                category: 'validation',
                type: 'validation',
              })}
              onDismiss={() => setShowWarning(false)}
            />
          )}
          {showInfo && (
            <ErrorDisplay
              error={createAppError('E009', 'Portfolio inquiry completed successfully', 'info')}
              onDismiss={() => setShowInfo(false)}
            />
          )}
        </div>
      </section>

      <section className="mb-8">
        <h2 className="mb-4 text-xl font-medium text-neutral-100">Toast Notifications</h2>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => handleToast('error')}
            className="rounded-pill border-2 border-error-red-100 px-4 py-2 text-sm font-bold text-error-red-100 hover:bg-error-red-20 transition-colors"
          >
            Error Toast
          </button>
          <button
            type="button"
            onClick={() => handleToast('warning')}
            className="rounded-pill border-2 border-warning-yellow-100 px-4 py-2 text-sm font-bold text-neutral-100 hover:bg-warning-yellow-20 transition-colors"
          >
            Warning Toast
          </button>
          <button
            type="button"
            onClick={() => handleToast('info')}
            className="rounded-pill border-2 border-action-blue-100 px-4 py-2 text-sm font-bold text-action-blue-100 hover:bg-action-blue-10 transition-colors"
          >
            Info Toast
          </button>
        </div>
      </section>
    </div>
  )
}

function App() {
  return (
    <ErrorBoundary>
      <ErrorDemo />
      <ToastContainer />
    </ErrorBoundary>
  )
}

export default App
