import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import ToastNotification from '../ToastNotification';

describe('ToastNotification', () => {
  it('renders the message', () => {
    render(
      <ToastNotification
        id="t1"
        message="Something happened"
        severity="info"
        onDismiss={() => {}}
      />,
    );
    expect(screen.getByText('Something happened')).toBeInTheDocument();
  });

  it('calls onDismiss when dismiss button is clicked', async () => {
    const onDismiss = vi.fn();
    const user = userEvent.setup();
    render(
      <ToastNotification
        id="t1"
        message="Msg"
        severity="error"
        onDismiss={onDismiss}
      />,
    );
    await user.click(screen.getByTestId('toast-dismiss'));
    expect(onDismiss).toHaveBeenCalledWith('t1');
  });

  it('auto-dismisses after the configured duration', () => {
    vi.useFakeTimers();
    const onDismiss = vi.fn();
    render(
      <ToastNotification
        id="t2"
        message="Auto"
        severity="success"
        duration={3000}
        onDismiss={onDismiss}
      />,
    );
    expect(onDismiss).not.toHaveBeenCalled();
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    expect(onDismiss).toHaveBeenCalledWith('t2');
    vi.useRealTimers();
  });

  it('has role="status" for accessibility', () => {
    render(
      <ToastNotification
        id="t3"
        message="Status"
        severity="warning"
        onDismiss={() => {}}
      />,
    );
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
