import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../../App';
import { renderWithProviders } from '../../test/renderWithProviders';

function setup() {
  const user = userEvent.setup();
  renderWithProviders(<App />, { route: '/positions' });
  return { user };
}

async function search(user: ReturnType<typeof userEvent.setup>, account: string) {
  await user.type(screen.getByLabelText('Account number'), account);
  await user.click(screen.getByRole('button', { name: 'Search' }));
}

describe('PositionInquiryPage', () => {
  it('prompts for an account before any search', () => {
    setup();
    expect(
      screen.getByText(/Enter an account number and search/i),
    ).toBeInTheDocument();
  });

  it('lists positions with valuation summary and paginates (PF7/PF8 replacement)', async () => {
    const { user } = setup();
    await search(user, 'ACCT100001');

    // First page: 10 of 12 holdings, ordered by investment id.
    expect(await screen.findByText('FND0000001')).toBeInTheDocument();
    expect(screen.getByText('FND0000010')).toBeInTheDocument();
    expect(screen.queryByText('FND0000012')).not.toBeInTheDocument();

    // Valuation summary is present.
    const summary = screen.getByTestId('valuation-summary');
    expect(within(summary).getByText('Total Market Value')).toBeInTheDocument();
    expect(within(summary).getByText('Gain / Loss')).toBeInTheDocument();

    expect(screen.getByText('12 positions')).toBeInTheDocument();
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();

    // Next page (replaces PF8).
    await user.click(screen.getByRole('button', { name: /Next/ }));
    expect(await screen.findByText('FND0000012')).toBeInTheDocument();
    expect(screen.queryByText('FND0000001')).not.toBeInTheDocument();
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument();
  });

  it('filters by status', async () => {
    const { user } = setup();
    await user.type(screen.getByLabelText('Account number'), 'ACCT100001');
    await user.selectOptions(screen.getByLabelText('Status'), 'P');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(await screen.findByText('FND0000005')).toBeInTheDocument();
    expect(screen.getByText('FND0000011')).toBeInTheDocument();
    expect(screen.getByText('2 positions')).toBeInTheDocument();
    expect(screen.queryByText('FND0000001')).not.toBeInTheDocument();
  });

  it('shows an empty state for an account with no positions', async () => {
    const { user } = setup();
    await search(user, 'ACCT999999');
    expect(await screen.findByTestId('empty-state')).toHaveTextContent(
      'No positions found for account ACCT999999',
    );
  });
});
