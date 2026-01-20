/**
 * Transaction Model
 * Migrated from: TRNREC.cpy, TRANSACTION_HISTORY DB2 table
 * 
 * Represents a financial transaction within a portfolio.
 */

import { TransactionType, TransactionStatus, CurrencyCode } from '../types';

/**
 * Transaction key structure
 * From: TRNREC.cpy (TRN-KEY)
 */
export interface TransactionKey {
  /** Transaction date (YYYYMMDD format) */
  transactionDate: string;
  /** Transaction time (HHMMSS format) */
  transactionTime: string;
  /** Portfolio identifier (8 characters) */
  portfolioId: string;
  /** Sequence number for multiple transactions (6 characters) */
  sequenceNumber: string;
}

/**
 * Transaction data
 * From: TRNREC.cpy (TRN-DATA)
 */
export interface TransactionData {
  /** Investment identifier (10 characters) */
  investmentId: string;
  /** Transaction type: Buy, Sell, Transfer, or Fee */
  transactionType: TransactionType;
  /** Transaction quantity (up to 11 digits with 4 decimal places) */
  quantity: number;
  /** Transaction price per unit (up to 11 digits with 4 decimal places) */
  price: number;
  /** Transaction amount (up to 13 digits with 2 decimal places) */
  amount: number;
  /** Currency code */
  currencyCode: CurrencyCode;
  /** Transaction status */
  status: TransactionStatus;
}

/**
 * Transaction audit information
 * From: TRNREC.cpy (TRN-AUDIT)
 */
export interface TransactionAuditInfo {
  /** Process timestamp */
  processDate: Date;
  /** User who processed the transaction */
  processUser: string;
}

/**
 * Complete Transaction record
 * Combines all transaction information from TRNREC.cpy and TRANSACTION_HISTORY table
 */
export interface Transaction {
  /** Transaction key */
  key: TransactionKey;
  /** Transaction data */
  data: TransactionData;
  /** Audit information */
  auditInfo: TransactionAuditInfo;
}

/**
 * Transaction with generated ID
 * Extended transaction with unique identifier
 */
export interface TransactionWithId extends Transaction {
  /** Unique transaction ID (YYYYMMDDHHMMSS + 6-digit sequence) */
  transactionId: string;
}

/**
 * Transaction creation request
 */
export interface CreateTransactionRequest {
  portfolioId: string;
  investmentId: string;
  transactionType: TransactionType;
  quantity: number;
  price: number;
  currencyCode: CurrencyCode;
}

/**
 * Transaction update request
 * Only status can be updated after creation
 */
export interface UpdateTransactionRequest {
  transactionId: string;
  status: TransactionStatus;
}

/**
 * Transaction search criteria
 */
export interface TransactionSearchCriteria {
  portfolioId?: string;
  investmentId?: string;
  transactionType?: TransactionType;
  status?: TransactionStatus;
  transactionDateFrom?: string;
  transactionDateTo?: string;
  minAmount?: number;
  maxAmount?: number;
  currencyCode?: CurrencyCode;
}

/**
 * Transaction summary for list views
 */
export interface TransactionSummary {
  transactionId: string;
  portfolioId: string;
  investmentId: string;
  transactionDate: string;
  transactionType: TransactionType;
  quantity: number;
  price: number;
  amount: number;
  status: TransactionStatus;
}

/**
 * Transaction history page result
 * Used for paginated transaction queries
 */
export interface TransactionHistoryPage {
  transactions: TransactionSummary[];
  totalCount: number;
  pageNumber: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Transaction validation result
 * From: TRNVAL00 batch program validation logic
 */
export interface TransactionValidationResult {
  isValid: boolean;
  errorCode: string | null;
  errorMessage: string | null;
  warnings: string[];
}

/**
 * Factory function to create a default Transaction object
 */
export function createDefaultTransaction(): Transaction {
  const now = new Date();
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
  const timeStr = now.toISOString().slice(11, 19).replace(/:/g, '');

  return {
    key: {
      transactionDate: dateStr,
      transactionTime: timeStr,
      portfolioId: '',
      sequenceNumber: '000001',
    },
    data: {
      investmentId: '',
      transactionType: TransactionType.BUY,
      quantity: 0,
      price: 0,
      amount: 0,
      currencyCode: CurrencyCode.USD,
      status: TransactionStatus.PENDING,
    },
    auditInfo: {
      processDate: new Date(),
      processUser: '',
    },
  };
}

/**
 * Generate a unique transaction ID
 * Format: YYYYMMDDHHMMSS + 6-digit sequence
 */
export function generateTransactionId(sequenceNumber: number): string {
  const now = new Date();
  const dateTimePart = now.toISOString()
    .replace(/[-:T]/g, '')
    .slice(0, 14);
  const sequencePart = sequenceNumber.toString().padStart(6, '0');
  return `${dateTimePart}${sequencePart}`;
}

/**
 * Calculate transaction amount from quantity and price
 */
export function calculateTransactionAmount(
  quantity: number,
  price: number,
  transactionType: TransactionType
): number {
  const baseAmount = quantity * price;
  
  switch (transactionType) {
    case TransactionType.BUY:
      return -Math.abs(baseAmount); // Debit (negative)
    case TransactionType.SELL:
      return Math.abs(baseAmount); // Credit (positive)
    case TransactionType.FEE:
      return -Math.abs(baseAmount); // Debit (negative)
    case TransactionType.TRANSFER:
      return baseAmount; // Can be either
    default:
      return baseAmount;
  }
}

/**
 * Get transaction type display name
 */
export function getTransactionTypeDisplayName(type: TransactionType): string {
  switch (type) {
    case TransactionType.BUY:
      return 'Buy';
    case TransactionType.SELL:
      return 'Sell';
    case TransactionType.TRANSFER:
      return 'Transfer';
    case TransactionType.FEE:
      return 'Fee';
    default:
      return 'Unknown';
  }
}

/**
 * Get transaction status display name
 */
export function getTransactionStatusDisplayName(status: TransactionStatus): string {
  switch (status) {
    case TransactionStatus.PENDING:
      return 'Pending';
    case TransactionStatus.DONE:
      return 'Completed';
    case TransactionStatus.FAILED:
      return 'Failed';
    case TransactionStatus.REVERSED:
      return 'Reversed';
    default:
      return 'Unknown';
  }
}
