import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { ErrorProvider, useErrors } from './ErrorContext';

function TestConsumer() {
  const { errors, addError, dismissError, clearErrors } = useErrors();
  return (
    <div>
      <span data-testid="count">{errors.length}</span>
      <ul>
        {errors.map((e) => (
          <li key={e.id} data-testid={`error-${e.id}`}>
            {e.message}
          </li>
        ))}
      </ul>
      <button onClick={() => addError({ code: 'E01', category: 'SY', severity: 'error', message: 'System error' })}>
        add-error
      </button>
      <button onClick={() => addError({ code: 'W01', category: 'VL', severity: 'warning', message: 'Warn' })}>
        add-warning
      </button>
      <button onClick={() => { if (errors[0]) dismissError(errors[0].id); }}>dismiss</button>
      <button onClick={clearErrors}>clear</button>
    </div>
  );
}

describe('ErrorContext', () => {
  it('throws when useErrors is used outside provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow('useErrors must be used within an ErrorProvider');
    spy.mockRestore();
  });

  it('starts with empty errors', () => {
    render(<ErrorProvider><TestConsumer /></ErrorProvider>);
    expect(screen.getByTestId('count')).toHaveTextContent('0');
  });

  it('addError adds an error', async () => {
    const user = userEvent.setup();
    render(<ErrorProvider><TestConsumer /></ErrorProvider>);
    await user.click(screen.getByText('add-error'));
    expect(screen.getByTestId('count')).toHaveTextContent('1');
    expect(screen.getByText('System error')).toBeInTheDocument();
  });

  it('dismissError removes an error', async () => {
    const user = userEvent.setup();
    render(<ErrorProvider><TestConsumer /></ErrorProvider>);
    await user.click(screen.getByText('add-error'));
    expect(screen.getByTestId('count')).toHaveTextContent('1');
    await user.click(screen.getByText('dismiss'));
    expect(screen.getByTestId('count')).toHaveTextContent('0');
  });

  it('clearErrors removes all errors', async () => {
    const user = userEvent.setup();
    render(<ErrorProvider><TestConsumer /></ErrorProvider>);
    await user.click(screen.getByText('add-error'));
    await user.click(screen.getByText('add-error'));
    expect(screen.getByTestId('count')).toHaveTextContent('2');
    await user.click(screen.getByText('clear'));
    expect(screen.getByTestId('count')).toHaveTextContent('0');
  });

  it('auto-dismisses warnings after 5 seconds', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime.bind(vi) });
    render(<ErrorProvider><TestConsumer /></ErrorProvider>);
    await user.click(screen.getByText('add-warning'));
    expect(screen.getByTestId('count')).toHaveTextContent('1');
    act(() => { vi.advanceTimersByTime(5100); });
    expect(screen.getByTestId('count')).toHaveTextContent('0');
    vi.useRealTimers();
  });

  it('does not auto-dismiss non-warning errors', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime.bind(vi) });
    render(<ErrorProvider><TestConsumer /></ErrorProvider>);
    await user.click(screen.getByText('add-error'));
    act(() => { vi.advanceTimersByTime(10000); });
    expect(screen.getByTestId('count')).toHaveTextContent('1');
    vi.useRealTimers();
  });
});
