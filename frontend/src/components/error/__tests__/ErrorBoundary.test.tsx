import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ErrorBoundary } from '@/components/error/ErrorBoundary'
import type { AppError } from '@/types/errors'
import type { ErrorInfo } from 'react'

function ThrowingChild({ shouldThrow }: { shouldThrow: boolean }) {
  if (shouldThrow) {
    throw new Error('Test render error')
  }
  return <div>Child content</div>
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <div>Hello world</div>
      </ErrorBoundary>,
    )
    expect(screen.getByText('Hello world')).toBeInTheDocument()
  })

  it('catches render errors and displays default ErrorDisplay fallback', () => {
    render(
      <ErrorBoundary>
        <ThrowingChild shouldThrow={true} />
      </ErrorBoundary>,
    )
    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('Test render error')).toBeInTheDocument()
    expect(screen.queryByText('Child content')).not.toBeInTheDocument()
  })

  it('calls onError callback with AppError and ErrorInfo when error occurs', () => {
    const onError = vi.fn()

    render(
      <ErrorBoundary onError={onError}>
        <ThrowingChild shouldThrow={true} />
      </ErrorBoundary>,
    )

    expect(onError).toHaveBeenCalledOnce()

    const [appError, errorInfo] = onError.mock.calls[0] as [AppError, ErrorInfo]
    expect(appError.code).toBe('RENDER_ERROR')
    expect(appError.message).toBe('Test render error')
    expect(appError.severity).toBe('error')
    expect(appError.category).toBe('system')
    expect(appError.type).toBe('processing')
    expect(errorInfo).toBeDefined()
  })

  it('supports custom fallback ReactNode', () => {
    render(
      <ErrorBoundary fallback={<div>Custom fallback</div>}>
        <ThrowingChild shouldThrow={true} />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Custom fallback')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('supports custom fallback render function that receives (error, reset)', () => {
    const fallbackFn = vi.fn((error: AppError, reset: () => void) => (
      <div>
        <span>Error: {error.message}</span>
        <button onClick={reset}>Reset</button>
      </div>
    ))

    render(
      <ErrorBoundary fallback={fallbackFn}>
        <ThrowingChild shouldThrow={true} />
      </ErrorBoundary>,
    )

    expect(fallbackFn).toHaveBeenCalled()
    expect(screen.getByText('Error: Test render error')).toBeInTheDocument()
    expect(screen.getByText('Reset')).toBeInTheDocument()

    const lastCall = fallbackFn.mock.calls[fallbackFn.mock.calls.length - 1] as [AppError, () => void]
    expect(lastCall[0].code).toBe('RENDER_ERROR')
    expect(typeof lastCall[1]).toBe('function')
  })

  it('reset function clears error state and re-renders children', () => {
    let shouldThrow = true

    function ConditionalThrower() {
      if (shouldThrow) {
        throw new Error('Conditional error')
      }
      return <div>Recovered content</div>
    }

    const fallbackFn = (_error: AppError, reset: () => void) => (
      <div>
        <span>Something went wrong</span>
        <button onClick={() => { shouldThrow = false; reset() }}>Try again</button>
      </div>
    )

    render(
      <ErrorBoundary fallback={fallbackFn}>
        <ConditionalThrower />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Something went wrong')).toBeInTheDocument()

    fireEvent.click(screen.getByText('Try again'))

    expect(screen.getByText('Recovered content')).toBeInTheDocument()
    expect(screen.queryByText('Something went wrong')).not.toBeInTheDocument()
  })

  it('handles multiple sequential errors after reset', () => {
    let errorMessage = 'First error'
    let shouldThrow = true

    function MultiThrower() {
      if (shouldThrow) {
        throw new Error(errorMessage)
      }
      return <div>Finally recovered</div>
    }

    const fallbackFn = (error: AppError, reset: () => void) => (
      <div>
        <span>Fallback: {error.message}</span>
        <button onClick={reset}>Reset</button>
      </div>
    )

    render(
      <ErrorBoundary fallback={fallbackFn}>
        <MultiThrower />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Fallback: First error')).toBeInTheDocument()

    errorMessage = 'Second error'
    fireEvent.click(screen.getByText('Reset'))

    expect(screen.getByText('Fallback: Second error')).toBeInTheDocument()

    shouldThrow = false
    fireEvent.click(screen.getByText('Reset'))

    expect(screen.getByText('Finally recovered')).toBeInTheDocument()
  })
})
