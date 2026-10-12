import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { currentPath, renderApp } from '../../test/renderApp';

describe('S2 Portfolio Position Inquiry', () => {
  it('FR-007: focuses the account field and rejects bad input client-side without calling the API', async () => {
    const { user } = renderApp('/positions');
    const field = await screen.findByLabelText('Account:');
    expect(field).toHaveFocus();
    for (const bad of ['0000000000', '12345678901', 'ABC']) {
      await user.clear(field);
      await user.type(field, `${bad}{Enter}`);
      expect(screen.getByRole('alert')).toHaveTextContent('Invalid Account Number format');
      expect(field).toHaveAttribute('aria-invalid', 'true');
      expect(field).toHaveFocus();
    }
    expect(currentPath()).toBe('/positions');
  });

  it('FR-007: an invalid account in the URL shows the validation error', async () => {
    renderApp('/positions/0000000000');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Invalid Account Number format'));
  });

  it('FR-008/FR-009: a valid account puts the account in the URL and shows the single position summary (FR-010, FR-011)', async () => {
    const { user } = renderApp('/positions');
    await user.type(await screen.findByLabelText('Account:'), '1000000002{Enter}');
    const summary = await screen.findByTestId('position-summary');
    expect(currentPath()).toBe('/positions/1000000002');
    expect(within(summary).getByText('MSFT000001')).toBeInTheDocument();
    expect(within(summary).getByText('120.0000')).toBeInTheDocument();
    expect(within(summary).getByText('$45,600.00')).toBeInTheDocument();
    expect(within(summary).getByText('$49,680.00')).toBeInTheDocument();
    expect(within(summary).getByText('PORT00002')).toBeInTheDocument();
    expect(within(summary).getByText('2024-03-20')).toBeInTheDocument();
  });

  it('FR-012: a missing fund name renders a dash with accessible text', async () => {
    renderApp('/positions/1000000002');
    const summary = await screen.findByTestId('position-summary');
    expect(within(summary).getByText('—')).toBeInTheDocument();
    expect(within(summary).getByText('Not available')).toBeInTheDocument();
  });

  it('FR-016: closed positions are shown with a Closed badge', async () => {
    renderApp('/positions/1000000003');
    const summary = await screen.findByTestId('position-summary');
    expect(within(summary).getByText('Closed')).toBeInTheDocument();
  });

  it('FR-014 (GAP-09 default): multiple positions render as a table, sorted by fund ID, with paging', async () => {
    const { user } = renderApp('/positions/1000000005');
    const table = await screen.findByTestId('positions-table');
    expect(within(table).getAllByRole('row')).toHaveLength(11);
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Next/ }));
    await waitFor(() => expect(currentPath()).toBe('/positions/1000000005?page=2'));
    expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument();
  });

  it('FR-013: not found shows the legacy message inline, keeps the account and focus', async () => {
    renderApp('/positions/1000000004');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Position not found for account'));
    expect(screen.getByLabelText('Account:')).toHaveValue('1000000004');
    expect(screen.queryByTestId('position-summary')).not.toBeInTheDocument();
  });

  it('FR-015: a read error shows "Error accessing position data" with a trace ID', async () => {
    renderApp('/positions/9999999998');
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Error accessing position data'));
    const alert = screen.getByRole('alert');
    expect(within(alert).getByTestId('trace-id').textContent).toMatch(/\S+/);
  });

  it('FR-029: a fatal error opens the System Error screen', async () => {
    renderApp('/positions/9999999999');
    expect(await screen.findByRole('heading', { name: /System Error/ })).toBeInTheDocument();
    expect(screen.getByTestId('error-code')).toHaveTextContent('SYSTEM_ERROR');
  });

  it('FR-026: Back to menu returns to S1 and keeps the selected account for the next screen (FR-008)', async () => {
    const { user } = renderApp('/positions/1000000002');
    await screen.findByTestId('position-summary');
    await user.click(screen.getByRole('link', { name: /Back to menu/ }));
    expect(currentPath()).toBe('/');
    await user.click(screen.getByRole('link', { name: '2. Transaction History' }));
    expect(await screen.findByLabelText('Account:')).toHaveValue('1000000002');
  });

  it('FR-026: Escape returns to the menu (PF3 replacement)', async () => {
    const { user } = renderApp('/positions/1000000002');
    await screen.findByTestId('position-summary');
    await user.keyboard('{Escape}');
    expect(currentPath()).toBe('/');
  });
});
