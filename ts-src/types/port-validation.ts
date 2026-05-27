/**
 * Portfolio Validation types and constants.
 * Migrated from: src/copybook/common/PORTVAL.cpy
 *
 * Validation return codes, error messages, and boundary constants
 * used by the PORTVALD subroutine.
 */

/** Validation-specific return codes. */
export enum ValidationReturnCode {
  Success = 0,
  InvalidId = 1,
  InvalidAccount = 2,
  InvalidType = 3,
  InvalidAmount = 4,
}

/** Canned error messages for each validation failure. */
export const VAL_ERR_ID = 'Invalid Portfolio ID format';
export const VAL_ERR_ACCT = 'Invalid Account Number format';
export const VAL_ERR_TYPE = 'Invalid Investment Type';
export const VAL_ERR_AMT = 'Amount is outside valid range';

/** Validation boundary constants. */
export const VAL_MIN_AMOUNT = 0.01;
export const VAL_MAX_AMOUNT = 9999999999999.99;
export const VAL_ID_PREFIX = 'PORT';

/** Valid investment types. */
export const VALID_INVESTMENT_TYPES = ['STK', 'BND', 'MMF', 'ETF'] as const;
export type InvestmentType = (typeof VALID_INVESTMENT_TYPES)[number];

/** Work area used during validation. */
export interface ValidationWorkArea {
  valNumericCheck: string;
  valTempNum: number;
}

/** Validation request (LINKAGE SECTION equivalent). */
export interface ValidationRequest {
  /** PIC X(1) – I=ID, A=Account, T=Type, M=Amount. */
  validateType: 'I' | 'A' | 'T' | 'M';
  /** PIC X(50) – The value to validate. */
  inputValue: string;
  /** Populated on return. */
  returnCode: ValidationReturnCode;
  /** Populated on return. */
  errorMsg: string;
}
