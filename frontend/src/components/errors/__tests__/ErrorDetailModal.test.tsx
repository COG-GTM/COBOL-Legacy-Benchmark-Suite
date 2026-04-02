import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import ErrorDetailModal from '../ErrorDetailModal';

describe('ErrorDetailModal', () => {
  it('renders nothing when open is false', () => {
    const { container } = render(
      <ErrorDetailModal
        open={false}
        errorCode="ERR001"
        errorDetails="Details"
        onClose={() => {}}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders error code and details when open', () => {
    render(
      <ErrorDetailModal
        open={true}
        errorCode="SYSERR01"
        errorDetails="Database connection failed"
        onClose={() => {}}
      />,
    );
    expect(screen.getByTestId('error-modal-code')).toHaveTextContent('SYSERR01');
    expect(screen.getByTestId('error-modal-details')).toHaveTextContent(
      'Database connection failed',
    );
  });

  it('calls onClose when Continue button is clicked', async () => {
    const onClose = vi.fn();
    const user = userEvent.setup();
    render(
      <ErrorDetailModal
        open={true}
        errorCode="E1"
        errorDetails="D1"
        onClose={onClose}
      />,
    );
    await user.click(screen.getByTestId('error-modal-continue'));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('calls onClose when Escape key is pressed', async () => {
    const onClose = vi.fn();
    const user = userEvent.setup();
    render(
      <ErrorDetailModal
        open={true}
        errorCode="E2"
        errorDetails="D2"
        onClose={onClose}
      />,
    );
    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('has role="dialog" and aria-modal', () => {
    render(
      <ErrorDetailModal
        open={true}
        errorCode="E3"
        errorDetails="D3"
        onClose={() => {}}
      />,
    );
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
  });

  it('calls onClose when clicking the overlay', async () => {
    const onClose = vi.fn();
    const user = userEvent.setup();
    render(
      <ErrorDetailModal
        open={true}
        errorCode="E4"
        errorDetails="D4"
        onClose={onClose}
      />,
    );
    await user.click(screen.getByTestId('error-modal-overlay'));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
