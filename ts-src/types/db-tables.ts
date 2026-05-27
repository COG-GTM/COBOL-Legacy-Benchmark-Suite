/**
 * DB2 Table Record types.
 * Migrated from: src/copybook/db2/DBTBLS.cpy
 *
 * Host-variable structures used when reading/writing the POSHIST and ERRLOG tables.
 */

/** Position History record (POSHIST table). */
export interface PosHistRecord {
  /** PIC X(10) – Account number. */
  phAccountNo: string;
  /** PIC X(8) – Portfolio ID. */
  phPortfolioId: string;
  /** PIC X(10) – Transaction date. */
  phTransDate: string;
  /** PIC X(8) – Transaction time. */
  phTransTime: string;
  /** PIC X(4) – Transaction type. */
  phTransType: string;
  /** PIC X(8) – Security / investment identifier. */
  phSecurityId: string;
  /** PIC S9(15)V999 COMP-3 – Quantity. */
  phQuantity: number;
  /** PIC S9(15)V999 COMP-3 – Price. */
  phPrice: number;
  /** PIC S9(15)V99 COMP-3 – Amount. */
  phAmount: number;
  /** PIC S9(15)V99 COMP-3 – Fees. */
  phFees: number;
  /** PIC S9(15)V99 COMP-3 – Total amount (amount + fees). */
  phTotalAmount: number;
  /** PIC S9(15)V99 COMP-3 – Cost basis. */
  phCostBasis: number;
  /** PIC S9(15)V99 COMP-3 – Realized gain/loss. */
  phGainLoss: number;
  /** PIC X(8) – Process date. */
  phProcessDate: string;
  /** PIC X(8) – Process time. */
  phProcessTime: string;
  /** PIC X(8) – Processing user ID. */
  phUserId: string;
}

/** Error type codes. */
export enum ErrorType {
  System = 'S',
  Application = 'A',
  Data = 'D',
}

/** Error Log record (ERRLOG table). */
export interface ErrLogRecord {
  /** PIC X(26) – Error timestamp. */
  elErrorTimestamp: string;
  /** PIC X(8) – Program that raised the error. */
  elProgramId: string;
  /** PIC X(1) – S/A/D. */
  elErrorType: ErrorType | string;
  /** PIC S9(4) – 1-4 severity level. */
  elErrorSeverity: number;
  /** PIC X(8) – Error code. */
  elErrorCode: string;
  /** PIC X(80) – Error message. */
  elErrorMessage: string;
  /** PIC X(10) – Process date. */
  elProcessDate: string;
  /** PIC X(8) – Process time. */
  elProcessTime: string;
  /** PIC X(8) – User ID. */
  elUserId: string;
  /** PIC X(100) – Additional diagnostic info. */
  elAdditionalInfo: string;
}
