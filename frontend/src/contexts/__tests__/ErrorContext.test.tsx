import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { ErrorProvider } from '../ErrorContext';
import { useError } from '../useError';

/** Helper that exposes the hook values via buttons & text. */
function TestConsumer() {
  const {
    inlineErrors,
    setInlineError,
    clearInlineError,
    modal,
    showErrorModal,
    dismissErrorModal,
    offline,
    setOffline,
  } = useError();

  return (
    <div>
      <span data-testid="inline">{JSON.stringify(inlineErrors)}</span>
      <span data-testid="modal">{JSON.stringify(modal)}</span>
      <span data-testid="offline">{String(offline)}</span>

      <button onClick={() => setInlineError('field1', 'Required')}>setInline</button>
      <button onClick={() => clearInlineError('field1')}>clearInline</button>
      <button onClick={() => showErrorModal('ERR01', 'Details here')}>showModal</button>
      <button onClick={() => dismissErrorModal()}>dismissModal</button>
      <button onClick={() => setOffline(true)}>goOffline</button>
      <button onClick={() => setOffline(false)}>goOnline</button>
    </div>
  );
}

const renderWithProvider = () =>
  render(
    <ErrorProvider>
      <TestConsumer />
    </ErrorProvider>,
  );

describe('ErrorContext', () => {
  it('provides default state', () => {
    renderWithProvider();
    expect(screen.getByTestId('inline')).toHaveTextContent('{}');
    expect(screen.getByTestId('modal')).toHaveTextContent(
      JSON.stringify({ open: false, code: '', details: '' }),
    );
    expect(screen.getByTestId('offline')).toHaveTextContent('false');
  });

  it('sets and clears inline errors', async () => {
    const user = userEvent.setup();
    renderWithProvider();

    await user.click(screen.getByText('setInline'));
    expect(screen.getByTestId('inline')).toHaveTextContent(
      JSON.stringify({ field1: 'Required' }),
    );

    await user.click(screen.getByText('clearInline'));
    expect(screen.getByTestId('inline')).toHaveTextContent('{}');
  });

  it('shows and dismisses the error modal', async () => {
    const user = userEvent.setup();
    renderWithProvider();

    await user.click(screen.getByText('showModal'));
    expect(screen.getByTestId('modal')).toHaveTextContent(
      JSON.stringify({ open: true, code: 'ERR01', details: 'Details here' }),
    );

    await user.click(screen.getByText('dismissModal'));
    expect(screen.getByTestId('modal')).toHaveTextContent(
      JSON.stringify({ open: false, code: '', details: '' }),
    );
  });

  it('toggles offline state', async () => {
    const user = userEvent.setup();
    renderWithProvider();

    await user.click(screen.getByText('goOffline'));
    expect(screen.getByTestId('offline')).toHaveTextContent('true');

    await user.click(screen.getByText('goOnline'));
    expect(screen.getByTestId('offline')).toHaveTextContent('false');
  });

  it('throws when useError is used outside provider', () => {
    // Suppress React error boundary console output
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow(
      'useError must be used within an <ErrorProvider>',
    );
    spy.mockRestore();
  });
});
