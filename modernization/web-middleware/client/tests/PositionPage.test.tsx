import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import PositionPage from '../src/pages/PositionPage';

const found = {
  commarea: { inqcomFunction: 'INQP', inqcomAccountNo: '100000001 ', inqcomResponseCode: 0, inqcomErrorMsg: ' '.repeat(80) },
  position: {
    posPortfolioId: 'PORT0001',
    posDate: '20240320',
    posInvestmentId: 'IBM001',
    posFundName: 'GLOBAL TECHNOLOGY FUND',
    posQuantity: 12500,
    posCostBasis: 12345678.99,
    posMarketValue: 13980221.45,
    posCurrency: 'SGD',
    posStatus: 'A',
    posLastMaintDate: '2024-03-20-15.30.45.123456',
    posLastMaintUser: 'BATCHUSR',
  },
};

const notFound = {
  commarea: {
    inqcomFunction: 'INQP',
    inqcomAccountNo: '999999999 ',
    inqcomResponseCode: 12,
    inqcomErrorMsg: 'Position not found for account'.padEnd(80),
  },
  position: null,
};

describe('PositionPage (POSMAP)', () => {
  it('renders the POSMAP labels and position data', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: async () => found }));
    render(<PositionPage />);
    expect(screen.getByText('Portfolio Position Inquiry')).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Account:'), '100000001');
    await userEvent.click(screen.getByRole('button', { name: 'Inquire' }));

    expect(await screen.findByText('IBM001')).toBeInTheDocument();
    expect(screen.getByText('GLOBAL TECHNOLOGY FUND')).toBeInTheDocument();
    ['Fund ID:', 'Fund Name:', 'Units:', 'Cost Basis:', 'Market Value:'].forEach((label) =>
      expect(screen.getByText(label)).toBeInTheDocument()
    );
  });

  it('shows the not-found error payload (P900-NOT-FOUND)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: async () => notFound }));
    render(<PositionPage />);
    await userEvent.type(screen.getByLabelText('Account:'), '999999999');
    await userEvent.click(screen.getByRole('button', { name: 'Inquire' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Position not found for account');
  });
});
