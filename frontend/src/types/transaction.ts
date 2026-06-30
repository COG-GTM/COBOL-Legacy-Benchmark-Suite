/**
 * Domain types for the Transaction Record.
 *
 * These mirror the COBOL copybook `src/copybook/common/TRNREC.cpy`
 * (01 TRANSACTION-RECORD). COBOL hyphenated names are translated to camelCase
 * while preserving the underlying semantics and 88-level allowed values, so the
 * structure can later map 1:1 to the backend PORTTRAN program.
 */

/** TRN-TYPE (X(2)) — 88-level values from TRNREC.cpy. */
export type TransactionType = 'BU' | 'SL' | 'TR' | 'FE';

/** TRN-STATUS (X(1)) — 88-level values from TRNREC.cpy. */
export type TransactionStatus = 'P' | 'D' | 'F' | 'R';

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  BU: 'Buy',
  SL: 'Sell',
  TR: 'Transfer',
  FE: 'Fee',
};

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  P: 'Pending',
  D: 'Done',
  F: 'Failed',
  R: 'Reversed',
};

/**
 * A single transaction. Monetary fields use the COMP-3 decimal-string
 * convention (see types/portfolio.ts and utils/decimal.ts) to preserve packed
 * decimal precision end to end.
 */
export interface Transaction {
  /** TRN-DATE PIC X(8) — YYYYMMDD. */
  date: string;
  /** TRN-TIME PIC X(6) — HHMMSS. */
  time: string;
  /** TRN-PORTFOLIO-ID. */
  portfolioId: string;
  /** TRN-SEQUENCE-NO. */
  sequenceNo: string;
  /** TRN-INVESTMENT-ID. */
  investmentId: string;
  /** TRN-TYPE. */
  type: TransactionType;
  /** TRN-AMOUNT — signed decimal string with 2 fraction digits. */
  amount: string;
  /** TRN-CURRENCY PIC X(3). */
  currency: string;
  /** TRN-STATUS. */
  status: TransactionStatus;
}
