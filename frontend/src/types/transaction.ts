/**
 * Transaction history interface derived from HISMAP in src/maps/INQSET.bms lines 53-85
 * and src/copybook/common/HISTREC.cpy
 */

export type TransactionType = 'BUY' | 'SELL' | 'TRANSFER' | 'FEE';
export type InvestmentType = 'STK' | 'BND' | 'MMF' | 'ETF';

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BUY: 'Buy',
  SELL: 'Sell',
  TRANSFER: 'Transfer',
  FEE: 'Fee',
};

export const INVESTMENT_TYPE_LABELS: Record<InvestmentType, string> = {
  STK: 'Stock',
  BND: 'Bond',
  MMF: 'Money Market Fund',
  ETF: 'Exchange Traded Fund',
};

export interface TransactionHistory {
  date: string;
  type: TransactionType;
  units: number;
  price: number;
  amount: number;
}

export interface TransactionEntry {
  portfolioId: string;
  accountNo: string;
  transactionType: TransactionType;
  investmentType: InvestmentType;
  units: number;
  amount: number;
}
