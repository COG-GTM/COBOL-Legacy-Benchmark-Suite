import { describe, expect, it, vi } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from './App';
import { HISTORY_FIXTURE } from './data/history.fixture';
import { MockHistoryService } from './services/mockHistoryService';
import { renderWithProviders } from './test/renderWithProviders';

function setup(route = '/history') {
  const service = new MockHistoryService();
  const user = userEvent.setup();
  renderWithProviders(<App />, { route, historyService: service });
  return { service, user };
}

async function searchAccount(user: ReturnType<typeof userEvent.setup>, account: string) {
  await user.type(screen.getByLabelText('Account number'), account);
  await user.click(screen.getByRole('button', { name: 'Search' }));
}

function bodyRows() {
  const table = screen.getByRole('table');
  return within(table).getAllByRole('row').slice(1);
}

describe('Transaction history inquiry', () => {
  it('lists an account history newest first, ten rows to a page', async () => {
    const { user } = setup();
    await searchAccount(user, 'ACCT100001');

    expect(await screen.findByText('24 history records')).toBeInTheDocument();
    expect(bodyRows()).toHaveLength(10);
    expect(within(bodyRows()[0]).getByRole('link')).toHaveTextContent(
      '2024-04-08',
    );

    await user.click(screen.getByRole('button', { name: /Next/ }));
    expect(screen.getByText('Page 2 of 3')).toBeInTheDocument();
    expect(within(bodyRows()[0]).getByRole('link')).toHaveTextContent(
      '2024-03-04',
    );
  });

  it('filters by date range and record type', async () => {
    const { user } = setup();
    await user.type(screen.getByLabelText('Account number'), 'ACCT100001');
    await user.type(screen.getByLabelText('Start date'), '2024-03-01');
    await user.type(screen.getByLabelText('End date'), '2024-03-31');
    await user.selectOptions(screen.getByLabelText('Record type'), 'TR');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    const rows = await screen.findAllByRole('row');
    const dates = rows
      .slice(1)
      .map((row) => within(row).getByRole('link').textContent);
    expect(dates.length).toBeGreaterThan(0);
    dates.forEach((date) => expect(date).toMatch(/^2024-03-/));
  });

  it('rejects a start date after the end date', async () => {
    const { user } = setup();
    await user.type(screen.getByLabelText('Account number'), 'ACCT100001');
    await user.type(screen.getByLabelText('Start date'), '2024-04-01');
    await user.type(screen.getByLabelText('End date'), '2024-01-01');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The start date must not be after the end date.',
    );
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('reports an account with no history', async () => {
    const { user } = setup();
    await searchAccount(user, 'ACCT999999');

    expect(await screen.findByTestId('empty-state')).toHaveTextContent(
      'No history found for account ACCT999999.',
    );
  });

  it('drills down to the record detail on row click', async () => {
    const { user } = setup();
    await searchAccount(user, 'ACCT100006');
    await user.click(await screen.findByRole('link', { name: '2024-03-05' }));

    expect(
      await screen.findByRole('heading', { name: 'History Record Detail' }),
    ).toBeInTheDocument();
    expect(screen.getByText('FND0000004')).toBeInTheDocument();
    expect(screen.getByText('Sell')).toBeInTheDocument();
    expect(screen.getByText('$491,167.44')).toBeInTheDocument();
    expect(
      screen.getByText(/UNITS=3251.7050\|PRICE=151.0492/),
    ).toBeInTheDocument();
  });

  it('reports an unknown record key on the detail route', async () => {
    setup('/history/NOSUCHKEY');
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'was not found',
    );
  });

  it('exports the current result set to CSV', async () => {
    const createObjectURL = vi.fn((_blob: Blob) => 'blob:history');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL });
    const downloads: string[] = [];
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(function (this: HTMLAnchorElement) {
        downloads.push(this.download);
      });

    const { user } = setup();
    const exportButton = screen.getByRole('button', { name: 'Export CSV' });
    expect(exportButton).toBeDisabled();

    await searchAccount(user, 'ACCT100006');
    await user.type(screen.getByLabelText('Start date'), '2024-01-01');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    await screen.findByRole('table');
    await user.click(exportButton);

    expect(downloads).toEqual(['history-ACCT100006-20240101.csv']);
    expect(createObjectURL.mock.calls[0][0].type).toBe(
      'text/csv;charset=utf-8;',
    );
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:history');

    clickSpy.mockRestore();
    vi.unstubAllGlobals();
  });

  it('keys every fixture row uniquely (HIST-KEY)', () => {
    const keys = HISTORY_FIXTURE.map(
      (r) => `${r.portfolioId}${r.date}${r.time}${r.seqNo}`,
    );
    expect(new Set(keys).size).toBe(keys.length);
  });
});
