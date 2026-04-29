import { ValidationCode, type ValidationResult, VALIDATION_MESSAGES } from '../types/validation';
import type { InvestmentType } from '../types/transaction';

/**
 * Validation utilities mirroring PORTVALD.cbl rules
 * See src/programs/portfolio/PORTVALD.cbl
 */

const VALID_INVESTMENT_TYPES: InvestmentType[] = ['STK', 'BND', 'MMF', 'ETF'];
const MIN_AMOUNT = 0.01;
const MAX_AMOUNT = 999999999.99;

function makeResult(code: ValidationCode): ValidationResult {
  return { code, message: VALIDATION_MESSAGES[code] };
}

/**
 * Portfolio ID must start with 'PORT' and have 4 numeric digits
 * Mirrors PORTVALD.cbl lines 52-70
 */
export function validatePortfolioId(id: string): ValidationResult {
  if (!id || id.length !== 8) {
    return makeResult(ValidationCode.INVALID_ID);
  }
  const prefix = id.substring(0, 4);
  const digits = id.substring(4, 8);
  if (prefix !== 'PORT' || !/^\d{4}$/.test(digits)) {
    return makeResult(ValidationCode.INVALID_ID);
  }
  return makeResult(ValidationCode.SUCCESS);
}

/**
 * Account number must be 10 numeric digits, not all zeros
 * Mirrors PORTVALD.cbl lines 74-86
 */
export function validateAccountNumber(accountNo: string): ValidationResult {
  if (!accountNo || accountNo.length !== 10 || !/^\d{10}$/.test(accountNo)) {
    return makeResult(ValidationCode.INVALID_ACCOUNT);
  }
  if (accountNo === '0000000000') {
    return makeResult(ValidationCode.INVALID_ACCOUNT);
  }
  return makeResult(ValidationCode.SUCCESS);
}

/**
 * Investment type must be one of STK, BND, MMF, ETF
 * Mirrors PORTVALD.cbl lines 90-103
 */
export function validateInvestmentType(type: string): ValidationResult {
  if (!VALID_INVESTMENT_TYPES.includes(type as InvestmentType)) {
    return makeResult(ValidationCode.INVALID_TYPE);
  }
  return makeResult(ValidationCode.SUCCESS);
}

/**
 * Amount must be within min/max bounds
 * Mirrors PORTVALD.cbl lines 107-120
 */
export function validateAmount(amount: number): ValidationResult {
  if (isNaN(amount) || amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
    return makeResult(ValidationCode.INVALID_AMOUNT);
  }
  return makeResult(ValidationCode.SUCCESS);
}
