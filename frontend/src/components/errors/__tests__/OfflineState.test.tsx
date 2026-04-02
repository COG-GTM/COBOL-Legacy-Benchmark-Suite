import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import OfflineState from '../OfflineState';

describe('OfflineState', () => {
  it('displays the offline message', () => {
    render(<OfflineState onRetry={() => {}} />);
    expect(
      screen.getByText('System is temporarily unavailable. Please try again later.'),
    ).toBeInTheDocument();
  });

  it('renders a Retry button', () => {
    render(<OfflineState onRetry={() => {}} />);
    expect(screen.getByText('Retry')).toBeInTheDocument();
  });

  it('calls onRetry when the Retry button is clicked', async () => {
    const onRetry = vi.fn();
    const user = userEvent.setup();
    render(<OfflineState onRetry={onRetry} />);
    await user.click(screen.getByTestId('offline-retry'));
    expect(onRetry).toHaveBeenCalledOnce();
  });

  it('has role="alert" for accessibility', () => {
    render(<OfflineState onRetry={() => {}} />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });
});
