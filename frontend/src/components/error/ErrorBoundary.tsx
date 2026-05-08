import { Component, type ErrorInfo, type ReactNode } from 'react'
import { ErrorDisplay } from './ErrorDisplay'
import type { AppError } from '@/types/errors'
import { createAppError } from '@/types/errors'

interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode | ((error: AppError, reset: () => void) => ReactNode)
  onError?: (error: AppError, errorInfo: ErrorInfo) => void
}

interface ErrorBoundaryState {
  error: AppError | null
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return {
      error: createAppError('RENDER_ERROR', error.message, 'error', {
        category: 'system',
        type: 'processing',
        details: error.stack,
      }),
    }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    const appError = createAppError('RENDER_ERROR', error.message, 'error', {
      category: 'system',
      type: 'processing',
      details: errorInfo.componentStack ?? error.stack,
    })

    this.props.onError?.(appError, errorInfo)
  }

  handleReset = (): void => {
    this.setState({ error: null })
  }

  render(): ReactNode {
    const { error } = this.state
    const { children, fallback } = this.props

    if (error) {
      if (typeof fallback === 'function') {
        return fallback(error, this.handleReset)
      }

      if (fallback) {
        return fallback
      }

      return (
        <ErrorDisplay
          error={error}
          onDismiss={this.handleReset}
          onRetry={this.handleReset}
        />
      )
    }

    return children
  }
}
