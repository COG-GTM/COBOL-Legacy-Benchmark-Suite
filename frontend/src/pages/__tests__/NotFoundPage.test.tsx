import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { NotFoundPage } from '@/pages/NotFoundPage'

describe('NotFoundPage', () => {
  it('renders 404 heading', () => {
    render(<NotFoundPage />)
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('404')
  })

  it("renders 'Page Not Found' subheading", () => {
    render(<NotFoundPage />)
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      'Page Not Found',
    )
  })

  it('renders descriptive message text', () => {
    render(<NotFoundPage />)
    expect(
      screen.getByText(
        /the page you are looking for does not exist or has been moved/i,
      ),
    ).toBeInTheDocument()
  })

  it('renders Return Home button when onNavigateHome is provided', () => {
    const onNavigateHome = vi.fn()
    render(<NotFoundPage onNavigateHome={onNavigateHome} />)
    expect(
      screen.getByRole('button', { name: /return home/i }),
    ).toBeInTheDocument()
  })

  it('calls onNavigateHome when Return Home button is clicked', () => {
    const onNavigateHome = vi.fn()
    render(<NotFoundPage onNavigateHome={onNavigateHome} />)
    fireEvent.click(screen.getByRole('button', { name: /return home/i }))
    expect(onNavigateHome).toHaveBeenCalledOnce()
  })

  it('does not render Return Home button when onNavigateHome is not provided', () => {
    render(<NotFoundPage />)
    expect(
      screen.queryByRole('button', { name: /return home/i }),
    ).not.toBeInTheDocument()
  })

  it("has role='main' for accessibility", () => {
    render(<NotFoundPage />)
    expect(screen.getByRole('main')).toBeInTheDocument()
  })
})
