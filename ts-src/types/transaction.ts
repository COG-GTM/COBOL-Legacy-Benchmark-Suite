/**
 * Transaction Record types.
 * Migrated from: src/copybook/common/TRNREC.cpy
 *
 * Describes buy/sell/transfer/fee transactions against portfolios.
 * Composite key: TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO.
 */

/** Transaction type codes (level-88). */
export enum TransactionType {
  Buy = 'BU',
  Sell = 'SL',
  Transfer = 'TR',
  Fee = 'FE',
}

/** Transaction processing status (level-88). */
export enum TransactionStatus {
  Pending = 'P',
  Done = 'D',
  Failed = 'F',
  Reversed = 'R',
}

/** Composite key for a transaction record. */
export interface TransactionKey {
  /** PIC X(8) – Transaction date (YYYYMMDD). */
  trnDate: string;
  /** PIC X(6) – Transaction time (HHMMSS). */
  trnTime: string;
  /** PIC X(8) – Owning portfolio. */
  trnPortfolioId: string;
  /** PIC 9(4) – Sequence within date/time/portfolio. */
  trnSequenceNo: number;
}

/** Full transaction record. */
export interface TransactionRecord {
  trnKey: TransactionKey;
  /** PIC X(2) – BU/SL/TR/FE. */
  trnType: TransactionType | string;
  /** PIC X(1) – P/D/F/R. */
  trnStatus: TransactionStatus | string;
  /** PIC X(8) – Security / investment identifier. */
  trnInvestmentId: string;
  /** PIC S9(11)V99 COMP-3 – Quantity of units. */
  trnQuantity: number;
  /** PIC S9(11)V99 COMP-3 – Price per unit. */
  trnPrice: number;
  /** PIC S9(13)V99 COMP-3 – Total transaction amount. */
  trnAmount: number;
  /** PIC S9(11)V99 COMP-3 – Fees / commissions. */
  trnFees: number;
  /** PIC X(10) – Account number reference. */
  trnAccountNo: string;
  /** PIC X(30) – Free-text description. */
  trnDescription: string;
}
