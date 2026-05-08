import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ToastContainer } from '@/components/error/ToastContainer'
import { toast, clearAllToasts } from '@/hooks/useToast'

describe('ToastContainer', () => {
  beforeEach(() => {
    clearAllToasts()
  })

  afterEach(() => {
    clearAllToasts()
  })

  it('renders nothing when there are no toasts', () => {
    const { container } = render(<ToastContainer />)
    expect(container.innerHTML).toBe('')
  })

  it('renders toast notifications when toasts exist', () => {
    toast('Test notification', 'info', { duration: 0 })
    render(<ToastContainer />)
    expect(screen.getByText('Test notification')).toBeInTheDocument()
  })

  it('each toast has an ErrorDisplay with dismiss button', () => {
    toast('Dismissable toast', 'error', { duration: 0 })
    render(<ToastContainer />)
    expect(screen.getByText('Dismissable toast')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument()
  })

  it('dismissing a toast removes it from the container', () => {
    toast('Will be dismissed', 'warning', { duration: 0 })
    render(<ToastContainer />)
    expect(screen.getByText('Will be dismissed')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }))
    expect(screen.queryByText('Will be dismissed')).not.toBeInTheDocument()
  })

  it("has aria-live='polite' and aria-label='Notifications'", () => {
    toast('Accessible toast', 'info', { duration: 0 })
    render(<ToastContainer />)
    const container = screen.getByLabelText('Notifications')
    expect(container).toHaveAttribute('aria-live', 'polite')
  })

  it('multiple toasts stack vertically', () => {
    toast('First toast', 'info', { duration: 0 })
    toast('Second toast', 'warning', { duration: 0 })
    toast('Third toast', 'error', { duration: 0 })
    render(<ToastContainer />)

    expect(screen.getByText('First toast')).toBeInTheDocument()
    expect(screen.getByText('Second toast')).toBeInTheDocument()
    expect(screen.getByText('Third toast')).toBeInTheDocument()

    const container = screen.getByLabelText('Notifications')
    const toastElements = container.querySelectorAll('[role="status"]')
    expect(toastElements).toHaveLength(3)
  })
})
