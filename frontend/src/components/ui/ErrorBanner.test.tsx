import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { ErrorBanner } from './ErrorBanner';
import type { AppError } from '@/data/types';

const baseError: AppError = {
  id: 'err-1',
  code: 'SYS001',
  category: 'SY',
  severity: 'error',
  message: 'Something went wrong',
  timestamp: '2024-01-01T00:00:00Z',
};

describe('ErrorBanner', () => {
  it('renders error code and message', () => {
    render(<ErrorBanner error={baseError} />);
    expect(screen.getByText('SYS001')).toBeInTheDocument();
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('renders category label', () => {
    render(<ErrorBanner error={baseError} />);
    expect(screen.getByText('System')).toBeInTheDocument();
  });

  it('does not show details toggle when no details', () => {
    render(<ErrorBanner error={baseError} />);
    expect(screen.queryByText('Show details')).not.toBeInTheDocument();
  });

  it('shows and hides details when toggle is clicked', async () => {
    const user = userEvent.setup();
    const errorWithDetails: AppError = { ...baseError, details: 'Stack trace here' };
    render(<ErrorBanner error={errorWithDetails} />);
    expect(screen.queryByText('Stack trace here')).not.toBeInTheDocument();
    await user.click(screen.getByText('Show details'));
    expect(screen.getByText('Stack trace here')).toBeInTheDocument();
    await user.click(screen.getByText('Hide details'));
    expect(screen.queryByText('Stack trace here')).not.toBeInTheDocument();
  });

  it('calls onDismiss when dismiss button clicked', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    render(<ErrorBanner error={baseError} onDismiss={onDismiss} />);
    const dismissButtons = screen.getAllByRole('button');
    await user.click(dismissButtons[0]);
    expect(onDismiss).toHaveBeenCalledWith('err-1');
  });

  it('does not render dismiss button when dismissible is false', () => {
    const nonDismissible: AppError = { ...baseError, dismissible: false };
    render(<ErrorBanner error={nonDismissible} onDismiss={vi.fn()} />);
    expect(screen.queryAllByRole('button').filter(b => !b.textContent?.includes('details'))).toHaveLength(0);
  });

  it('renders different severity styles', () => {
    const { container, rerender } = render(<ErrorBanner error={{ ...baseError, severity: 'warning' }} />);
    expect(container.firstChild).toHaveClass('bg-amber-50');

    rerender(<ErrorBanner error={{ ...baseError, severity: 'critical' }} />);
    expect(container.firstChild).toHaveClass('bg-purple-50');

    rerender(<ErrorBanner error={{ ...baseError, severity: 'severe' }} />);
    expect(container.firstChild).toHaveClass('bg-orange-50');
  });
});
