import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import HistoryPage from '../src/pages/HistoryPage';

const row = (seq: string) => ({
  histPortfolioId: 'PORT0001',
  histDate: '20240301',
  histTime: '093045',
  histSeqNo: seq,
  histRecordType: 'TR',
  histActionCode: 'A',
  histInvestmentId: 'IBM001',
  histUnits: 18,
  histPrice: 22.5,
  histAmount: 405,
  histReasonCode: '0000',
  histProcessDate: '2024-03-01-15.30.45.123456',
  histProcessUser: 'BATCHUSR',
});

const payload = {
  commarea: { inqcomFunction: 'INQH', inqcomAccountNo: '100000001 ', inqcomResponseCode: 0, inqcomErrorMsg: ' '.repeat(80) },
  rows: [row('0001'), row('0002')],
  page: 1,
  pageSize: 10,
  totalRows: 23,
  totalPages: 3,
};

describe('HistoryPage (HISMAP)', () => {
  it('renders the history table headers, rows and PF7/PF8 pagination', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: async () => payload }));
    render(<HistoryPage />);
    expect(screen.getByText('Transaction History Inquiry')).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Account:'), '100000001');
    await userEvent.click(screen.getByRole('button', { name: 'Inquire' }));

    expect(await screen.findByText('Amount')).toBeInTheDocument();
    ['Date', 'Type', 'Units', 'Price'].forEach((h) => expect(screen.getByText(h)).toBeInTheDocument());
    expect(screen.getAllByText('2024-03-01')).toHaveLength(2);
    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'PF7 Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'PF8 Next' })).toBeEnabled();
  });
});
