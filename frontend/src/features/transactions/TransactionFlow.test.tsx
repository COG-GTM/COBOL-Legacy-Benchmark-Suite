import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../../App';
import { renderWithProviders } from '../../test/renderWithProviders';

function setup(route = '/transactions') {
  const user = userEvent.setup();
  renderWithProviders(<App />, { route });
  return { user };
}

async function fillForm(
  user: ReturnType<typeof userEvent.setup>,
  {
    type = 'BU',
    portfolioId = 'PORT0001',
    investmentId = 'FND0000001',
    quantity = '10',
    price = '400.50',
  }: Partial<Record<string, string>> = {},
) {
  await user.selectOptions(screen.getByLabelText(/Transaction Type/), type);
  await user.type(screen.getByLabelText(/Portfolio ID/), portfolioId);
  await user.type(screen.getByLabelText(/Investment ID/), investmentId);
  await user.type(screen.getByLabelText(/Quantity/), quantity);
  if (type !== 'TR') {
    await user.type(screen.getByLabelText(/Price per Unit/), price);
  }
}

describe('Transaction status view', () => {
  it('lists fixture transactions and filters by status', async () => {
    const { user } = setup();

    expect(await screen.findByText('10 transactions')).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Status'), 'P');
    await user.click(screen.getByRole('button', { name: 'Search' }));

    expect(await screen.findByText('3 transactions')).toBeInTheDocument();
    const rows = screen.getAllByTestId('status-badge');
    expect(rows.every((r) => r.textContent === 'Pending')).toBe(true);
  });
});

describe('Transaction submission', () => {
  it('auto-calculates the amount and requires confirmation before submitting', async () => {
    const { user } = setup('/transactions/new');
    await fillForm(user);

    expect(screen.getByTestId('computed-amount')).toHaveTextContent(
      '$4,005.00',
    );

    await user.click(screen.getByRole('button', { name: 'Review' }));

    const review = await screen.findByText('Review transaction');
    expect(review).toBeInTheDocument();
    expect(screen.getByText('Confirm & Submit')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Confirm & Submit/ }));

    // Lands on the status view with the new pending record.
    expect(
      await screen.findByText(/was\s+submitted and is pending settlement/),
    ).toBeInTheDocument();
    expect(await screen.findByText('11 transactions')).toBeInTheDocument();
  });

  it('blocks submission when required fields are missing', async () => {
    const { user } = setup('/transactions/new');
    await user.click(screen.getByRole('button', { name: 'Review' }));

    expect(
      await screen.findByText('Portfolio ID is required.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Quantity is required.')).toBeInTheDocument();
    expect(screen.queryByText('Review transaction')).not.toBeInTheDocument();
  });

  it('rejects a sell that exceeds the units held', async () => {
    const { user } = setup('/transactions/new');
    await fillForm(user, { type: 'SL', quantity: '99999' });
    await user.click(screen.getByRole('button', { name: 'Review' }));

    expect(
      await screen.findByText(/Insufficient units for sale: 1,250.5 available/),
    ).toBeInTheDocument();
    expect(screen.queryByText('Review transaction')).not.toBeInTheDocument();
  });

  it('hides price for transfers and reviews them with a zero amount', async () => {
    const { user } = setup('/transactions/new');
    await fillForm(user, { type: 'TR', quantity: '500' });

    expect(screen.getByLabelText(/Price per Unit/)).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Review' }));

    const review = await screen.findByText('Review transaction');
    const card = review.closest('.card') as HTMLElement;
    expect(within(card).getByText('Transfer')).toBeInTheDocument();
    expect(within(card).getByText('$0.00')).toBeInTheDocument();
  });

  it('surfaces an unknown portfolio reported by the service', async () => {
    const { user } = setup('/transactions/new');
    await fillForm(user, { portfolioId: 'PORT9999' });
    await user.click(screen.getByRole('button', { name: 'Review' }));
    await user.click(
      await screen.findByRole('button', { name: /Confirm & Submit/ }),
    );

    expect(
      await screen.findByText('Invalid Portfolio ID: PORT9999'),
    ).toBeInTheDocument();
  });
});
