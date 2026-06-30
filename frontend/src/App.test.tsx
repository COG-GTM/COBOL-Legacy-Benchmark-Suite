import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { App } from './App';

function renderApp(initialPath = '/') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <App />
    </MemoryRouter>,
  );
}

describe('App shell', () => {
  it('renders the dashboard with key metrics by default', () => {
    renderApp('/');
    expect(
      screen.getByRole('heading', { level: 1, name: 'Dashboard' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Total AUM')).toBeInTheDocument();
    expect(screen.getByText('Active Portfolios')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Recent Transactions' }),
    ).toBeInTheDocument();
  });

  it('exposes the primary navigation sections', () => {
    renderApp('/');
    const nav = screen.getByRole('navigation', { name: 'Primary' });
    for (const label of [
      'Dashboard',
      'Portfolios',
      'Transactions',
      'History',
      'Reports',
    ]) {
      expect(within(nav).getByRole('link', { name: new RegExp(label) }))
        .toBeInTheDocument();
    }
  });

  it('shows a consistent header and footer', () => {
    renderApp('/');
    expect(screen.getByText('Portfolio Management')).toBeInTheDocument();
    expect(
      screen.getByText(/Portfolio Management System/),
    ).toBeInTheDocument();
  });

  it('navigates to a section and updates the breadcrumb trail', async () => {
    const user = userEvent.setup();
    renderApp('/');
    const nav = screen.getByRole('navigation', { name: 'Primary' });
    await user.click(within(nav).getByRole('link', { name: /Reports/ }));

    expect(
      screen.getByRole('heading', { level: 1, name: 'Reports' }),
    ).toBeInTheDocument();
    const breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' });
    expect(within(breadcrumb).getByText('Reports')).toBeInTheDocument();
  });

  it('supports the g-then-key keyboard shortcut', async () => {
    const user = userEvent.setup();
    renderApp('/');
    await user.keyboard('gp');
    expect(
      screen.getByRole('heading', { level: 1, name: 'Portfolios' }),
    ).toBeInTheDocument();
  });

  it('renders a not-found page for unknown routes', () => {
    renderApp('/does-not-exist');
    expect(
      screen.getByRole('heading', { level: 1, name: 'Page not found' }),
    ).toBeInTheDocument();
  });
});
