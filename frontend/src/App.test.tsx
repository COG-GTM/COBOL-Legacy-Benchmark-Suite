import { describe, expect, it } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from './App';
import { MockPortfolioService } from './services/mockPortfolioService';
import { renderWithProviders } from './test/renderWithProviders';

function setup(route = '/portfolios') {
  const service = new MockPortfolioService();
  const user = userEvent.setup();
  renderWithProviders(<App />, { route, portfolioService: service });
  return { service, user };
}

describe('Portfolio CRUD app', () => {
  it('renders the portfolio list with status indicators', async () => {
    setup();
    expect(await screen.findByText('Margaret Chen')).toBeInTheDocument();
    expect(screen.getAllByTestId('status-badge').length).toBeGreaterThan(0);
    expect(screen.getByText(/\$12,503,488\.99/)).toBeInTheDocument();
  });

  it('filters the list by status', async () => {
    const { user } = setup();
    await screen.findByText('Margaret Chen');

    await user.selectOptions(screen.getByLabelText('Status'), 'C');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(
      await screen.findByText('Nexus Capital Partners'),
    ).toBeInTheDocument();
    expect(screen.queryByText('Margaret Chen')).not.toBeInTheDocument();
  });

  it('shows the detail view with all PORT-RECORD fields', async () => {
    setup('/portfolios/PORT0001');
    expect(
      await screen.findByRole('heading', { name: 'Margaret Chen' }),
    ).toBeInTheDocument();
    expect(screen.getByText(/PORT-ID/)).toBeInTheDocument();
    expect(screen.getByText(/PORT-CASH-BALANCE/)).toBeInTheDocument();
    expect(screen.getByText(/PORT-LAST-TRANS/)).toBeInTheDocument();
    expect(screen.getByText('Individual (I)')).toBeInTheDocument();
  });

  it('deletes a portfolio after confirmation', async () => {
    const { user, service } = setup('/portfolios/PORT0001');
    await screen.findByRole('heading', { name: 'Margaret Chen' });

    await user.click(screen.getByRole('button', { name: 'Delete' }));
    const dialog = screen.getByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await screen.findByText('Atlas Holdings LLC');
    expect(await service.get('PORT0001')).toBeUndefined();
  });

  it('validates the create form and creates a portfolio', async () => {
    const { user, service } = setup('/portfolios/new');

    await user.click(
      await screen.findByRole('button', { name: 'Create Portfolio' }),
    );
    expect(
      await screen.findByText('Portfolio ID is required.'),
    ).toBeInTheDocument();

    await user.type(screen.getByLabelText(/Portfolio ID/), 'PORT9000');
    await user.type(screen.getByLabelText(/Account Number/), 'ACCT900000');
    await user.type(screen.getByLabelText(/Client Name/), 'Jordan Lee');
    await user.selectOptions(screen.getByLabelText(/Client Type/), 'T');
    await user.type(screen.getByLabelText(/Total Value/), '4321.5');
    await user.type(screen.getByLabelText(/Cash Balance/), '100');

    await user.click(screen.getByRole('button', { name: 'Create Portfolio' }));

    await waitFor(async () =>
      expect(await service.get('PORT9000')).toBeDefined(),
    );
    const created = await service.get('PORT9000');
    expect(created?.totalValue).toBe('4321.50');
  });

  it('pre-populates the edit form and saves changes', async () => {
    const { user, service } = setup('/portfolios/PORT0004/edit');

    const nameInput = await screen.findByLabelText(/Client Name/);
    expect(nameInput).toHaveValue('Robert Okafor');
    expect(screen.getByLabelText(/Portfolio ID/)).toBeDisabled();

    await user.clear(nameInput);
    await user.type(nameInput, 'Robert O. Okafor');
    await user.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(async () =>
      expect((await service.get('PORT0004'))?.clientName).toBe(
        'Robert O. Okafor',
      ),
    );
  });
});
