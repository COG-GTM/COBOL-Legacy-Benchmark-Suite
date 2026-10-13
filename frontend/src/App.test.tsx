import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import App from './App';
import { createMockApi, DENIED_ACCOUNT, ERROR_ACCOUNT } from './api/mock';

async function setup() {
  const user = userEvent.setup();
  render(<App api={createMockApi(0)} />);
  await screen.findByRole('heading', { level: 1, name: 'Okafor Family Trust' });
  return user;
}

async function lookup(user: ReturnType<typeof userEvent.setup>, value: string) {
  const input = screen.getByLabelText('Account number');
  await user.clear(input);
  await user.type(input, value);
  await user.click(screen.getByRole('button', { name: 'Look up' }));
}

describe('account lookup', () => {
  it('loads the first account with header details', async () => {
    await setup();
    expect(screen.getByText('4471-0028-93')).toBeInTheDocument();
    expect(screen.getByText('Revocable trust')).toBeInTheDocument();
    expect(screen.getByText(/Prices as of Oct 13, 2026/)).toBeInTheDocument();
  });

  it('accepts separators and switches account', async () => {
    const user = await setup();
    await lookup(user, '4471-0055-12');
    expect(await screen.findByRole('heading', { level: 1, name: 'R. Lindqvist Rollover IRA' })).toBeInTheDocument();
  });

  it('validates input inline', async () => {
    const user = await setup();
    await user.click(screen.getByRole('button', { name: 'Look up' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a 10-digit account number');
    await lookup(user, 'abc');
    expect(screen.getByRole('alert')).toHaveTextContent('digits only');
    await lookup(user, '12345');
    expect(screen.getByRole('alert')).toHaveTextContent('10 digits');
  });

  it('keeps current account when lookup is not found', async () => {
    const user = await setup();
    await lookup(user, '9999999999');
    expect(await screen.findByRole('alert')).toHaveTextContent('No account found for 9999-9999-99');
    expect(screen.getByRole('heading', { level: 1, name: 'Okafor Family Trust' })).toBeInTheDocument();
  });

  it('shows access denied without account data', async () => {
    const user = await setup();
    await lookup(user, DENIED_ACCOUNT);
    expect(await screen.findByRole('heading', { name: 'Access denied' })).toBeInTheDocument();
    expect(screen.queryByTestId('kpis')).not.toBeInTheDocument();
  });

  it('shows system error details with retry', async () => {
    const user = await setup();
    await lookup(user, ERROR_ACCOUNT);
    expect(await screen.findByRole('heading', { name: 'System error' })).toBeInTheDocument();
    expect(screen.getByText('DB2-0904')).toBeInTheDocument();
    expect(screen.getByText('A7F3C2E19B04D611')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });
});

describe('navigation', () => {
  it('switches tabs by click and keyboard and syncs the hash', async () => {
    const user = await setup();
    expect(screen.getByRole('tab', { name: /Overview/ })).toHaveAttribute('aria-selected', 'true');
    await user.click(screen.getByRole('tab', { name: /Positions/ }));
    expect(window.location.hash).toBe('#positions');
    await user.keyboard('3');
    expect(screen.getByRole('tab', { name: /Transactions/ })).toHaveAttribute('aria-selected', 'true');
    expect(window.location.hash).toBe('#transactions');
    await user.keyboard('/');
    expect(screen.getByLabelText('Search transactions')).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.getByLabelText('Search transactions')).not.toHaveFocus();
  });
});

describe('positions', () => {
  it('filters by status and search, and sorts', async () => {
    const user = await setup();
    await user.click(screen.getByRole('tab', { name: /Positions/ }));
    const panel = screen.getByRole('tabpanel', { name: /Positions/ });
    const table = within(panel).getByRole('table', { name: 'Positions' });
    expect(within(table).getAllByRole('row')).toHaveLength(1 + 7 + 1);
    await user.click(within(panel).getByRole('button', { name: /^Closed/ }));
    expect(within(table).getByText('Meridian Small Cap Value')).toBeInTheDocument();
    expect(within(table).queryByText('Summit Tech Leaders')).not.toBeInTheDocument();
    await user.click(within(panel).getByRole('button', { name: /^All/ }));
    await user.type(within(panel).getByLabelText('Search positions'), 'hpbix');
    expect(within(table).getByText('Harbor Point Aggregate Bond')).toBeInTheDocument();
    expect(within(table).getAllByRole('row')).toHaveLength(1 + 1 + 1);
    await user.clear(within(panel).getByLabelText('Search positions'));
    await user.click(within(table).getByRole('button', { name: /Fund/ }));
    expect(within(table).getByRole('columnheader', { name: /Fund/ })).toHaveAttribute('aria-sort', 'ascending');
    const first = within(table).getAllByRole('row')[1];
    expect(first).toHaveTextContent('Altamira Emerging Markets');
  });

  it('remembers card layout', async () => {
    const user = await setup();
    await user.click(screen.getByRole('tab', { name: /Positions/ }));
    await user.click(screen.getByRole('button', { name: 'Cards' }));
    expect(screen.getByRole('article', { name: 'Northfield Total Market Index' })).toBeInTheDocument();
    expect(localStorage.getItem('portfolio-desk.positions-layout')).toBe('cards');
  });
});

describe('transactions', () => {
  it('paginates and filters', async () => {
    const user = await setup();
    await user.click(screen.getByRole('tab', { name: /Transactions/ }));
    const panel = screen.getByRole('tabpanel', { name: /Transactions/ });
    const rows = () => within(within(panel).getByRole('table')).getAllByRole('row').slice(1);
    expect(rows()).toHaveLength(10);
    const firstId = rows()[0].querySelector('td.mono')!.textContent;
    await user.keyboard(']');
    expect(within(panel).getByRole('button', { name: '2' })).toHaveAttribute('aria-current', 'page');
    expect(rows()[0].querySelector('td.mono')!.textContent).not.toBe(firstId);
    await user.selectOptions(within(panel).getByLabelText('Rows per page'), '25');
    expect(rows().length).toBeGreaterThan(10);
    await user.click(within(panel).getByRole('button', { name: 'Fee' }));
    rows().forEach((r) => expect(r).toHaveTextContent('Fee'));
    expect(within(panel).getByTestId('tx-summary')).toHaveTextContent(/of \d+ transactions/);
    await user.click(within(panel).getAllByRole('button', { name: 'Clear filters' })[0]);
    await user.selectOptions(within(panel).getByLabelText('Status'), 'Pending');
    rows().forEach((r) => expect(r).toHaveTextContent('Pending'));
    await user.type(within(panel).getByLabelText('Search transactions'), 'zzzz');
    expect(within(panel).getByText('No transactions match these filters')).toBeInTheDocument();
  });
});

describe('review fixes', () => {
  it('hides the previous account while a lookup is in flight', async () => {
    const user = userEvent.setup();
    render(<App api={createMockApi(30)} />);
    await screen.findByRole('heading', { level: 1, name: 'Okafor Family Trust' });
    await lookup(user, DENIED_ACCOUNT);
    expect(screen.queryByTestId('kpis')).not.toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Access denied' })).toBeInTheDocument();
  });

  it('retries the account list after a failure', async () => {
    const user = userEvent.setup();
    const mock = createMockApi(0);
    let calls = 0;
    const api = { ...mock, listAccounts: () => (++calls === 1 ? Promise.reject(new Error('down')) : mock.listAccounts()) };
    render(<App api={api} />);
    await user.click(await screen.findByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Okafor Family Trust' })).toBeInTheDocument();
  });
});
