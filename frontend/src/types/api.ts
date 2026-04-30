/**
 * API contract interfaces matching COBOL COMMAREA structures
 * Derived from src/copybook/online/INQCOM.cpy (INQCOM-AREA)
 */

export type InquiryFunction = 'MENU' | 'INQP' | 'INQH' | 'EXIT';

export interface InquiryRequest {
  function: InquiryFunction;
  accountNo: string;
}

export interface InquiryResponse {
  responseCode: number;
  errorMsg: string;
  data?: unknown;
}

export interface PortfolioListResponse {
  portfolios: import('./portfolio').Portfolio[];
  total: number;
  page: number;
  pageSize: number;
}

export interface PositionResponse {
  accountNo: string;
  positions: import('./position').Position[];
}

export interface TransactionHistoryResponse {
  accountNo: string;
  transactions: import('./transaction').TransactionHistory[];
  total: number;
  page: number;
  pageSize: number;
}

export interface TransactionSubmitRequest {
  portfolioId: string;
  accountNo: string;
  transactionType: import('./transaction').TransactionType;
  investmentType: import('./transaction').InvestmentType;
  units: number;
  amount: number;
}

export interface TransactionSubmitResponse {
  success: boolean;
  message: string;
  transactionId?: string;
}
