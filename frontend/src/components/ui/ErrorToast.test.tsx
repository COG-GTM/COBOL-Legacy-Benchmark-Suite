import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { ErrorToast } from './ErrorToast';
import { ErrorProvider, useErrors } from '@/context/ErrorContext';

function AddErrorButton() {
  const { addError } = useErrors();
  return (
    <button onClick={() => addError({ code: 'E01', category: 'SY', severity: 'error', message: 'Test error' })}>
      trigger
    </button>
  );
}

function TestHarness() {
  return (
    <ErrorProvider>
      <AddErrorButton />
      <ErrorToast />
    </ErrorProvider>
  );
}

describe('ErrorToast', () => {
  it('renders nothing when no errors exist', () => {
    const { container } = render(
      <ErrorProvider>
        <ErrorToast />
      </ErrorProvider>,
    );
    expect(container.querySelector('.fixed')).toBeNull();
  });

  it('renders toast when error is added', async () => {
    const user = userEvent.setup();
    render(<TestHarness />);
    await user.click(screen.getByText('trigger'));
    expect(screen.getByText('Test error')).toBeInTheDocument();
    expect(screen.getByText('E01')).toBeInTheDocument();
  });

  it('renders dismiss button for dismissible errors', async () => {
    const user = userEvent.setup();
    render(<TestHarness />);
    await user.click(screen.getByText('trigger'));
    const buttons = screen.getAllByRole('button');
    const dismissBtn = buttons.find((b) => b !== screen.getByText('trigger'));
    expect(dismissBtn).toBeDefined();
  });
});
