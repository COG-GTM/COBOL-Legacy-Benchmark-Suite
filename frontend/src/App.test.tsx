import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import App from './App';
import { MENU_OPTIONS } from './routes/functionCodes';

function renderApp(initialPath = '/') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <App />
    </MemoryRouter>,
  );
}

describe('Main menu', () => {
  it('renders the title and the three legacy menu options in order', () => {
    renderApp();

    expect(
      screen.getByRole('heading', { name: 'Portfolio Management System' }),
    ).toBeInTheDocument();

    const items = screen.getAllByRole('menuitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('1.Portfolio Position Inquiry');
    expect(items[1]).toHaveTextContent('2.Transaction History');
    expect(items[2]).toHaveTextContent('3.Exit');

    // Each option carries its legacy 4-char function code.
    expect(items[0]).toHaveAttribute('data-function-code', 'INQP');
    expect(items[1]).toHaveAttribute('data-function-code', 'INQH');
    expect(items[2]).toHaveAttribute('data-function-code', 'EXIT');
  });
});

describe('Menu navigation', () => {
  it('navigates to the Portfolio Position Inquiry screen (INQP)', async () => {
    const user = userEvent.setup();
    renderApp();

    await user.click(
      screen.getByRole('menuitem', { name: /Portfolio Position Inquiry/ }),
    );

    expect(
      screen.getByRole('heading', { name: 'Portfolio Position Inquiry' }),
    ).toBeInTheDocument();
    expect(screen.getByText(/coming soon/i)).toBeInTheDocument();
  });

  it('navigates to the Transaction History screen (INQH)', async () => {
    const user = userEvent.setup();
    renderApp();

    await user.click(
      screen.getByRole('menuitem', { name: /Transaction History/ }),
    );

    expect(
      screen.getByRole('heading', { name: 'Transaction History' }),
    ).toBeInTheDocument();
  });

  it('ends the session when Exit is selected (EXIT)', async () => {
    const user = userEvent.setup();
    renderApp();

    await user.click(screen.getByRole('menuitem', { name: /Exit/ }));

    expect(
      screen.getByRole('heading', { name: 'Session Ended' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(/session has ended/i);
  });

  it('returns to the menu via the Back to Menu link', async () => {
    const user = userEvent.setup();
    renderApp('/portfolio');

    await user.click(screen.getByRole('link', { name: /Back to Menu/ }));

    expect(
      screen.getByRole('heading', { name: 'Portfolio Management System' }),
    ).toBeInTheDocument();
  });

  it('exposes exactly one route per legacy function code', () => {
    // Guardrails so future backend wiring lines up with INQCOM-FUNCTION.
    const codes = MENU_OPTIONS.map((o) => o.code);
    expect(codes).toEqual(['INQP', 'INQH', 'EXIT']);
  });
});
