import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ErrorDisplay } from '@/components/error/ErrorDisplay'
import type { AppError } from '@/types/errors'

function makeError(overrides: Partial<AppError> = {}): AppError {
  return {
    code: 'TEST_ERR',
    message: 'Something went wrong',
    severity: 'error',
    ...overrides,
  }
}

describe('ErrorDisplay', () => {
  it('renders error severity with red styling, AlertCircle icon', () => {
    const { container } = render(<ErrorDisplay error={makeError({ severity: 'error' })} />)
    const alertDiv = container.firstElementChild as HTMLElement
    expect(alertDiv.className).toContain('border-error-red-100')
    expect(alertDiv.className).toContain('bg-error-red-20')
    expect(screen.getByText('Error')).toBeInTheDocument()
  })

  it('renders warning severity with yellow styling, AlertTriangle icon', () => {
    const { container } = render(<ErrorDisplay error={makeError({ severity: 'warning' })} />)
    const alertDiv = container.firstElementChild as HTMLElement
    expect(alertDiv.className).toContain('border-warning-yellow-100')
    expect(alertDiv.className).toContain('bg-warning-yellow-20')
    expect(screen.getByText('Warning')).toBeInTheDocument()
  })

  it('renders info severity with blue styling, Info icon', () => {
    const { container } = render(<ErrorDisplay error={makeError({ severity: 'info' })} />)
    const alertDiv = container.firstElementChild as HTMLElement
    expect(alertDiv.className).toContain('border-navy-100')
    expect(alertDiv.className).toContain('bg-action-blue-20')
    expect(screen.getByText('Information')).toBeInTheDocument()
  })

  it('displays error code in a code element', () => {
    render(<ErrorDisplay error={makeError({ code: 'E001' })} />)
    const codeEl = screen.getByText('E001')
    expect(codeEl.tagName).toBe('CODE')
  })

  it('displays error message text', () => {
    render(<ErrorDisplay error={makeError({ message: 'Record not found' })} />)
    expect(screen.getByText('Record not found')).toBeInTheDocument()
  })

  it('shows expandable details section when error.details is provided', () => {
    render(<ErrorDisplay error={makeError({ details: 'Stack trace info here' })} />)
    expect(screen.getByText('Show details')).toBeInTheDocument()
    expect(screen.getByText('Stack trace info here')).toBeInTheDocument()
  })

  it('hides details section when error.details is not provided', () => {
    render(<ErrorDisplay error={makeError({ details: undefined })} />)
    expect(screen.queryByText('Show details')).not.toBeInTheDocument()
  })

  it('calls onDismiss when dismiss button is clicked', () => {
    const onDismiss = vi.fn()
    render(<ErrorDisplay error={makeError()} onDismiss={onDismiss} />)
    fireEvent.click(screen.getByLabelText('Dismiss'))
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('calls onRetry when retry button is clicked', () => {
    const onRetry = vi.fn()
    render(<ErrorDisplay error={makeError()} onRetry={onRetry} />)
    fireEvent.click(screen.getByLabelText('Retry'))
    expect(onRetry).toHaveBeenCalledOnce()
  })

  it('hides dismiss button when onDismiss is not provided', () => {
    render(<ErrorDisplay error={makeError()} />)
    expect(screen.queryByLabelText('Dismiss')).not.toBeInTheDocument()
  })

  it('hides retry button when onRetry is not provided', () => {
    render(<ErrorDisplay error={makeError()} />)
    expect(screen.queryByLabelText('Retry')).not.toBeInTheDocument()
  })

  it("has role='alert' and aria-live='assertive' for accessibility", () => {
    render(<ErrorDisplay error={makeError()} />)
    const alert = screen.getByRole('alert')
    expect(alert).toHaveAttribute('aria-live', 'assertive')
  })

  it('dismiss and retry buttons have proper aria-labels', () => {
    render(<ErrorDisplay error={makeError()} onDismiss={() => {}} onRetry={() => {}} />)
    expect(screen.getByLabelText('Dismiss')).toBeInTheDocument()
    expect(screen.getByLabelText('Retry')).toBeInTheDocument()
  })
})
