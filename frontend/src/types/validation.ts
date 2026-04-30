/**
 * Validation result interface derived from src/copybook/common/PORTVAL.cpy
 * Error codes: 0=success, 1=invalid ID, 2=invalid account, 3=invalid type, 4=invalid amount
 */

export const ValidationCode = {
  SUCCESS: 0,
  INVALID_ID: 1,
  INVALID_ACCOUNT: 2,
  INVALID_TYPE: 3,
  INVALID_AMOUNT: 4,
} as const;

export type ValidationCode = (typeof ValidationCode)[keyof typeof ValidationCode];

export const VALIDATION_MESSAGES: Record<ValidationCode, string> = {
  [ValidationCode.SUCCESS]: 'Validation successful',
  [ValidationCode.INVALID_ID]: 'Invalid Portfolio ID format',
  [ValidationCode.INVALID_ACCOUNT]: 'Invalid Account Number format',
  [ValidationCode.INVALID_TYPE]: 'Invalid Investment Type',
  [ValidationCode.INVALID_AMOUNT]: 'Amount outside valid range',
};

export interface ValidationResult {
  code: ValidationCode;
  message: string;
}
