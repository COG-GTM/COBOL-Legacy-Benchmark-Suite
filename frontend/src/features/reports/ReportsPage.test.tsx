import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../../App';
import { renderWithProviders } from '../../test/renderWithProviders';

function setup(route = '/reports/positions') {
  const user = userEvent.setup();
  renderWithProviders(<App />, { route });
  return { user };
}

describe('Reports & Analytics', () => {
  it('lands on the position report from the Reports nav item', async () => {
    const { user } = setup('/portfolios');
    await user.click(screen.getByRole('link', { name: 'Reports' }));

    expect(
      await screen.findByRole('table', { name: 'Position report' }),
    ).toBeInTheDocument();
  });

  it('summarizes portfolio valuations in the position report (RPTPOS00)', async () => {
    setup();

    const table = await screen.findByRole('table', {
      name: 'Position report',
    });
    expect(within(table).getByText('Margaret Chen')).toBeInTheDocument();

    const summary = screen.getByTestId('valuation-summary');
    expect(within(summary).getByText('Total Market Value')).toBeInTheDocument();
    expect(within(summary).getByText('Gain / Loss')).toBeInTheDocument();
  });

  it('filters the position report by portfolio and keeps the filter across reports', async () => {
    const { user } = setup();
    await screen.findByRole('table', { name: 'Position report' });

    await user.selectOptions(screen.getByLabelText('Portfolio'), 'PORT0002');

    expect(await screen.findByText('Atlas Holdings LLC')).toBeInTheDocument();
    expect(screen.queryByText('Margaret Chen')).not.toBeInTheDocument();

    await user.click(screen.getByRole('link', { name: 'Audit' }));
    const auditTable = await screen.findByRole('table', {
      name: 'Audit report',
    });
    expect(within(auditTable).queryAllByText('PORT0001')).toHaveLength(0);
    expect(within(auditTable).getAllByText('PORT0002').length).toBeGreaterThan(
      0,
    );
  });

  it('filters the audit report by user and status (RPTAUD00)', async () => {
    const { user } = setup('/reports/audit');
    await screen.findByRole('table', { name: 'Audit report' });

    await user.selectOptions(screen.getByLabelText('User'), 'AHAMMETT');
    await user.selectOptions(screen.getByLabelText('Status'), 'FAIL');

    expect(await screen.findByTestId('empty-state')).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Status'), '');
    const table = await screen.findByRole('table', { name: 'Audit report' });
    const rows = within(table).getAllByRole('row').slice(1);
    expect(rows.length).toBeGreaterThan(0);
    for (const row of rows) {
      expect(within(row).getByText('AHAMMETT')).toBeInTheDocument();
    }
  });

  it('shows processing volumes and error rates (RPTSTA00)', async () => {
    setup('/reports/statistics');

    const summary = await screen.findByTestId('statistics-summary');
    expect(within(summary).getByText('DB2 Calls')).toBeInTheDocument();
    expect(within(summary).getByText('Error Rate')).toBeInTheDocument();
    expect(
      screen.getByRole('table', { name: 'Daily processing volumes' }),
    ).toBeInTheDocument();
  });

  it('compares return codes with the prior period (RTNANA00)', async () => {
    const { user } = setup('/reports/returns');

    const table = await screen.findByRole('table', {
      name: 'Return code analysis',
    });
    expect(within(table).getByText('PORTMSTR')).toBeInTheDocument();

    await user.type(screen.getByLabelText('From date'), '2024-03-29');
    await user.type(screen.getByLabelText('To date'), '2024-04-02');

    await waitFor(() => {
      expect(screen.getByTestId('comparison-period')).toHaveTextContent(
        '2024-03-29 – 2024-04-02 compared with 2024-03-24 – 2024-03-28',
      );
    });
  });

  it('exports the current report to CSV and PDF', async () => {
    const createObjectURL = vi.fn(() => 'blob:report');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL });
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => {});

    const { user } = setup();
    await screen.findByRole('table', { name: 'Position report' });

    await user.click(screen.getByRole('button', { name: 'Export CSV' }));
    await user.click(screen.getByRole('button', { name: 'Export PDF' }));

    expect(click).toHaveBeenCalledTimes(2);
    const [csvBlob] = createObjectURL.mock.calls[0] as unknown as [Blob];
    const [pdfBlob] = createObjectURL.mock.calls[1] as unknown as [Blob];
    expect(csvBlob.type).toContain('text/csv');
    expect(csvBlob.size).toBeGreaterThan(0);
    expect(pdfBlob.type).toBe('application/pdf');
    expect(pdfBlob.size).toBeGreaterThan(0);

    click.mockRestore();
    vi.unstubAllGlobals();
  });
});
