import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../../mocks/server';
import { currentPath, renderApp } from '../../test/renderApp';

const rows = () => screen.getAllByTestId('history-row');

describe('S3 Transaction History Inquiry', () => {
  it('FR-017/FR-018/FR-019/FR-022: shows 10 rows newest first with the doc columns and formats', async () => {
    renderApp('/history/1000000001?page=1');
    await screen.findByTestId('history-table');
    expect(rows()).toHaveLength(10);
    expect(screen.getAllByRole('columnheader').map((h) => h.textContent)).toEqual(['Date', 'Type', 'Units', 'Price', 'Amount', 'Details']);
    const first = within(rows()[0]!);
    expect(first.getByText('2024-03-20')).toBeInTheDocument();
    expect(first.getByText('BU – Buy')).toBeInTheDocument();
    expect(first.getByText('65.790')).toBeInTheDocument();
    expect(first.getByText('190.000')).toBeInTheDocument();
    expect(first.getByText('$12,500.00')).toBeInTheDocument();
    expect(within(rows()[1]!).getByText('-$25.00')).toBeInTheDocument();
    const dates = rows().map((r) => r.querySelector('th')!.textContent!);
    expect([...dates].sort().reverse()).toEqual(dates);
  });

  it('FR-020/FR-025: paging moves through pages, updates ?page and disables at the ends', async () => {
    const { user } = renderApp('/history/1000000001?page=1');
    await screen.findByTestId('history-table');
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Previous/ })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: /Next/ }));
    await waitFor(() => expect(currentPath()).toBe('/history/1000000001?page=2'));
    expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument();
    expect(rows()).toHaveLength(2);
    expect(screen.getByRole('button', { name: /Next/ })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: /Previous/ }));
    expect(await screen.findByText('Page 1 of 2')).toBeInTheDocument();
  });

  it('FR-020: PageDown / PageUp page through results (PF8 / PF7 replacement)', async () => {
    const { user } = renderApp('/history/1000000001?page=1');
    await screen.findByTestId('history-table');
    await user.keyboard('{PageDown}');
    expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument();
    await user.keyboard('{PageUp}');
    expect(await screen.findByText('Page 1 of 2')).toBeInTheDocument();
  });

  it('FR-025: a new search resets to page 1', async () => {
    const { user } = renderApp('/history/1000000001?page=2');
    expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument();
    const field = screen.getByLabelText('Account:');
    await user.clear(field);
    await user.type(field, '1000000002{Enter}');
    await waitFor(() => expect(currentPath()).toBe('/history/1000000002?page=1'));
    expect(await screen.findByText('Page 1 of 1')).toBeInTheDocument();
  });

  it('FR-021: expanding the 2024-03-15 SL row shows Gain/Loss $47.55', async () => {
    const { user } = renderApp('/history/1000000001?page=1');
    await screen.findByTestId('history-table');
    const row = rows().find((r) => r.textContent?.includes('2024-03-15'))!;
    await user.click(within(row).getByRole('button', { name: /Show details/ }));
    const details = screen.getByText('Gain/Loss').closest('dl')!;
    expect(within(details).getByText('$47.55')).toBeInTheDocument();
  });

  it('FR-008: arriving at /history after a position search loads that account without retyping', async () => {
    const { user } = renderApp('/positions/1000000001');
    await screen.findByTestId('positions-table');
    await user.click(screen.getByRole('link', { name: 'Transaction History' }));
    await waitFor(() => expect(currentPath()).toBe('/history/1000000001?page=1'));
    expect(await screen.findByTestId('history-table')).toBeInTheDocument();
  });

  it('FR-021: row details expose time, security, fees, totals, cost basis and gain/loss', async () => {
    const { user } = renderApp('/history/1000000002?page=1');
    await screen.findByTestId('history-table');
    const toggle = screen.getByRole('button', { name: /Show details/ });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    for (const label of ['Time', 'Security ID', 'Fees', 'Total Amount', 'Cost Basis', 'Gain/Loss']) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
  });

  it('FR-023: empty history shows an informational message, not an error', async () => {
    renderApp('/history/1000000004?page=1');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('No transactions found for account'));
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Information');
    expect(screen.queryByTestId('history-table')).not.toBeInTheDocument();
  });

  it('FR-007: invalid account on history is rejected before calling the API', async () => {
    const { user } = renderApp('/history');
    await user.type(await screen.findByLabelText('Account:'), '0000000000{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Invalid Account Number format');
  });

  it('FR-024: a 500 read error shows "Error accessing transaction history" with a trace ID', async () => {
    renderApp('/history/9999999998?page=1');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Error accessing transaction history'));
    const alert = screen.getByRole('alert');
    expect(within(alert).getByTestId('trace-id')).toBeInTheDocument();
  });

  it('FR-024: DB unavailable offers Retry which re-issues the request', async () => {
    const { user } = renderApp('/history/9999999997?page=1');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Transaction history is temporarily unavailable'));
    const alert = screen.getByRole('alert');
    let calls = 0;
    server.use(
      http.get('*/api/v1/accounts/:acct/transactions', () => {
        calls += 1;
        return HttpResponse.json({ items: [], page: 1, pageSize: 10, totalCount: 0, hasNext: false, hasPrevious: false });
      }),
    );
    await user.click(within(alert).getByRole('button', { name: 'Retry' }));
    await waitFor(() => expect(calls).toBe(1));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('No transactions found for account'));
  });
});
