export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';
export type TransactionStatus = 'P' | 'D' | 'F' | 'R';

export interface Transaction {
  transactionId: string;
  portfolioId: string;
  transactionDate: string;
  transactionTime: string;
  investmentId: string;
  transactionType: TransactionType;
  quantity: number;
  price: number;
  amount: number;
  currencyCode: string;
  status: TransactionStatus;
  processDate?: string;
  processUser?: string;
}
