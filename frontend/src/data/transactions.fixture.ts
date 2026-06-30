import type { Transaction } from '../types/transaction';

/**
 * Mock transaction data fixture.
 *
 * Each record mirrors the `01 TRANSACTION-RECORD` layout from
 * `src/copybook/common/TRNREC.cpy`. Monetary values use the COMP-3 decimal
 * string convention (see types/transaction.ts). This fixture stands in for the
 * transaction file until the backend API is connected.
 */
export const TRANSACTION_FIXTURE: Transaction[] = [
  {
    date: '20240401',
    time: '093015',
    portfolioId: 'PORT0007',
    sequenceNo: '000120',
    investmentId: 'EQ-AAPL',
    type: 'BU',
    amount: '125000.00',
    currency: 'USD',
    status: 'D',
  },
  {
    date: '20240401',
    time: '101142',
    portfolioId: 'PORT0002',
    sequenceNo: '000119',
    investmentId: 'BD-USTR10',
    type: 'SL',
    amount: '48250.50',
    currency: 'USD',
    status: 'D',
  },
  {
    date: '20240331',
    time: '154500',
    portfolioId: 'PORT0001',
    sequenceNo: '000118',
    investmentId: 'EQ-MSFT',
    type: 'BU',
    amount: '32100.00',
    currency: 'USD',
    status: 'P',
  },
  {
    date: '20240331',
    time: '140233',
    portfolioId: 'PORT0006',
    sequenceNo: '000117',
    investmentId: 'FND-IDX500',
    type: 'TR',
    amount: '18450.00',
    currency: 'USD',
    status: 'D',
  },
  {
    date: '20240330',
    time: '110800',
    portfolioId: 'PORT0004',
    sequenceNo: '000116',
    investmentId: 'MGMT-FEE',
    type: 'FE',
    amount: '395.20',
    currency: 'USD',
    status: 'D',
  },
  {
    date: '20240329',
    time: '163020',
    portfolioId: 'PORT0008',
    sequenceNo: '000115',
    investmentId: 'EQ-TSLA',
    type: 'SL',
    amount: '12700.00',
    currency: 'USD',
    status: 'F',
  },
];
