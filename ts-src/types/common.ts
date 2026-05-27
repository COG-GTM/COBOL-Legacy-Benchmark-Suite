/**
 * Common constants and shared types.
 * Migrated from: src/copybook/common/COMMON.cpy
 *
 * System-wide return codes, status values, transaction types, and utility fields.
 */

/** Standard return codes used across all programs. */
export enum ReturnCode {
  Success = 0,
  Warning = 4,
  Error = 8,
  Severe = 12,
  Critical = 16,
}

/** General record status values. */
export enum RecordStatus {
  Active = 'A',
  Inactive = 'I',
  Closed = 'C',
  Pending = 'P',
  Deleted = 'D',
}

/** Standard transaction type codes. */
export enum StandardTransactionType {
  Buy = 'BU',
  Sell = 'SL',
  Transfer = 'TR',
  Fee = 'FE',
  Dividend = 'DV',
  Interest = 'IN',
}

/** Currency codes. */
export enum CurrencyCode {
  USD = 'USD',
  EUR = 'EUR',
  GBP = 'GBP',
  JPY = 'JPY',
  CAD = 'CAD',
}

/** Common date/time work area. */
export interface DateTimeFields {
  /** PIC X(8) – YYYYMMDD. */
  currentDate: string;
  /** PIC X(6) – HHMMSS. */
  currentTime: string;
  /** PIC X(26) – Full ISO-style timestamp. */
  currentTimestamp: string;
}

/** Common error handling work area. */
export interface CommonErrorArea {
  errorFlag: boolean;
  errorProgram: string;
  errorMessage: string;
  errorCode: string;
}

/** Standard file status codes. */
export enum FileStatus {
  Success = '00',
  EndOfFile = '10',
  DuplicateKey = '22',
  RecordNotFound = '23',
  FileNotFound = '35',
}
