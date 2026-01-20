/**
 * Position History Model
 * Migrated from: POSHIST.sql DB2 table, HISTREC.cpy
 * 
 * Represents historical position and transaction data stored in DB2.
 */

import { TransactionType, HistoryRecordType, HistoryActionCode } from '../types';

/**
 * Position History record
 * From: POSHIST.sql DB2 table
 */
export interface PositionHistory {
  /** Account number (8 characters) */
  accountNumber: string;
  /** Portfolio identifier (10 characters) */
  portfolioId: string;
  /** Transaction date */
  transactionDate: Date;
  /** Transaction time */
  transactionTime: string;
  /** Transaction type: Buy, Sell, Transfer */
  transactionType: TransactionType;
  /** Security identifier (12 characters) */
  securityId: string;
  /** Transaction quantity */
  quantity: number;
  /** Transaction price */
  price: number;
  /** Transaction amount */
  amount: number;
  /** Transaction fees */
  fees: number;
  /** Total amount including fees */
  totalAmount: number;
  /** Cost basis amount */
  costBasis: number;
  /** Realized gain/loss amount */
  gainLoss: number;
  /** Process date */
  processDate: Date;
  /** Process time */
  processTime: string;
  /** Program ID that created the record */
  programId: string;
  /** User ID */
  userId: string;
  /** Audit timestamp */
  auditTimestamp: Date;
}

/**
 * History record for change tracking
 * From: HISTREC.cpy
 */
export interface HistoryRecord {
  /** History key */
  key: HistoryKey;
  /** History data */
  data: HistoryData;
  /** Audit information */
  auditInfo: HistoryAuditInfo;
}

/**
 * History key structure
 * From: HISTREC.cpy (HIST-KEY)
 */
export interface HistoryKey {
  /** Portfolio identifier (8 characters) */
  portfolioId: string;
  /** History date (YYYYMMDD format) */
  historyDate: string;
  /** History time (HHMMSS format) */
  historyTime: string;
  /** Sequence number (4 characters) */
  sequenceNumber: string;
}

/**
 * History data
 * From: HISTREC.cpy (HIST-DATA)
 */
export interface HistoryData {
  /** Record type: Portfolio, Position, or Transaction */
  recordType: HistoryRecordType;
  /** Action code: Add, Change, or Delete */
  actionCode: HistoryActionCode;
  /** Record image before change (up to 400 characters) */
  beforeImage: string;
  /** Record image after change (up to 400 characters) */
  afterImage: string;
  /** Reason code for the change (4 characters) */
  reasonCode: string;
}

/**
 * History audit information
 * From: HISTREC.cpy (HIST-AUDIT)
 */
export interface HistoryAuditInfo {
  /** Process timestamp */
  processDate: Date;
  /** User who made the change */
  processUser: string;
}

/**
 * Position history search criteria
 */
export interface PositionHistorySearchCriteria {
  accountNumber?: string;
  portfolioId?: string;
  securityId?: string;
  transactionType?: TransactionType;
  transactionDateFrom?: Date;
  transactionDateTo?: Date;
  processDateFrom?: Date;
  processDateTo?: Date;
  programId?: string;
  userId?: string;
}

/**
 * Position history summary for reports
 */
export interface PositionHistorySummary {
  portfolioId: string;
  securityId: string;
  transactionDate: Date;
  transactionType: TransactionType;
  quantity: number;
  amount: number;
  gainLoss: number;
}

/**
 * Position history page result
 * Used for paginated history queries
 */
export interface PositionHistoryPage {
  records: PositionHistory[];
  totalCount: number;
  pageNumber: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Position history aggregate for reporting
 */
export interface PositionHistoryAggregate {
  portfolioId: string;
  securityId: string;
  totalTransactions: number;
  totalBuys: number;
  totalSells: number;
  totalQuantityBought: number;
  totalQuantitySold: number;
  totalAmountBought: number;
  totalAmountSold: number;
  totalFees: number;
  totalRealizedGainLoss: number;
  periodStart: Date;
  periodEnd: Date;
}

/**
 * Factory function to create a default PositionHistory object
 */
export function createDefaultPositionHistory(): PositionHistory {
  const now = new Date();
  const timeStr = now.toISOString().slice(11, 19).replace(/:/g, '');

  return {
    accountNumber: '',
    portfolioId: '',
    transactionDate: now,
    transactionTime: timeStr,
    transactionType: TransactionType.BUY,
    securityId: '',
    quantity: 0,
    price: 0,
    amount: 0,
    fees: 0,
    totalAmount: 0,
    costBasis: 0,
    gainLoss: 0,
    processDate: now,
    processTime: timeStr,
    programId: '',
    userId: '',
    auditTimestamp: now,
  };
}

/**
 * Factory function to create a default HistoryRecord object
 */
export function createDefaultHistoryRecord(): HistoryRecord {
  const now = new Date();
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
  const timeStr = now.toISOString().slice(11, 19).replace(/:/g, '');

  return {
    key: {
      portfolioId: '',
      historyDate: dateStr,
      historyTime: timeStr,
      sequenceNumber: '0001',
    },
    data: {
      recordType: HistoryRecordType.TRANSACTION,
      actionCode: HistoryActionCode.ADD,
      beforeImage: '',
      afterImage: '',
      reasonCode: '',
    },
    auditInfo: {
      processDate: now,
      processUser: '',
    },
  };
}

/**
 * Calculate realized gain/loss for a sell transaction
 */
export function calculateRealizedGainLoss(
  sellAmount: number,
  costBasis: number,
  fees: number
): number {
  return sellAmount - costBasis - fees;
}

/**
 * Get history record type display name
 */
export function getHistoryRecordTypeDisplayName(type: HistoryRecordType): string {
  switch (type) {
    case HistoryRecordType.PORTFOLIO:
      return 'Portfolio';
    case HistoryRecordType.POSITION:
      return 'Position';
    case HistoryRecordType.TRANSACTION:
      return 'Transaction';
    default:
      return 'Unknown';
  }
}

/**
 * Get history action code display name
 */
export function getHistoryActionCodeDisplayName(code: HistoryActionCode): string {
  switch (code) {
    case HistoryActionCode.ADD:
      return 'Added';
    case HistoryActionCode.CHANGE:
      return 'Changed';
    case HistoryActionCode.DELETE:
      return 'Deleted';
    default:
      return 'Unknown';
  }
}
