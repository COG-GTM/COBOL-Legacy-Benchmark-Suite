/**
 * Portfolio Validation Service — migrated from PORTVALD.cbl + PORTVAL.cpy
 *
 * Validates portfolio data elements:
 *  - Portfolio ID format  (PORTnnnn)
 *  - Account number       (10 numeric digits, non-zero)
 *  - Investment type       (STK | BND | MMF | ETF)
 *  - Amount range          (within min/max bounds)
 */

import Decimal from "decimal.js";

// --- Return codes (VAL-RETURN-CODES) ---
export const ValidationCode = {
  SUCCESS: 0,
  INVALID_ID: 1,
  INVALID_ACCOUNT: 2,
  INVALID_TYPE: 3,
  INVALID_AMOUNT: 4,
} as const;

export type ValidationCodeValue =
  (typeof ValidationCode)[keyof typeof ValidationCode];

// --- Error messages (VAL-ERROR-MESSAGES) ---
export const ValidationMessage: Record<ValidationCodeValue, string> = {
  [ValidationCode.SUCCESS]: "",
  [ValidationCode.INVALID_ID]: "Invalid Portfolio ID format",
  [ValidationCode.INVALID_ACCOUNT]: "Invalid Account Number format",
  [ValidationCode.INVALID_TYPE]: "Invalid Investment Type",
  [ValidationCode.INVALID_AMOUNT]: "Amount outside valid range",
};

// --- Constants (VAL-CONSTANTS) ---
export const ID_PREFIX = "PORT";
export const VALID_INVESTMENT_TYPES = ["STK", "BND", "MMF", "ETF"] as const;
export type InvestmentType = (typeof VALID_INVESTMENT_TYPES)[number];
export const MIN_AMOUNT = new Decimal("-9999999999999.99");
export const MAX_AMOUNT = new Decimal("9999999999999.99");

export interface ValidationResult {
  code: ValidationCodeValue;
  message: string;
}

function ok(): ValidationResult {
  return { code: ValidationCode.SUCCESS, message: "" };
}

function fail(code: ValidationCodeValue): ValidationResult {
  return { code, message: ValidationMessage[code] };
}

/** 1000-VALIDATE-ID: Portfolio ID must start with 'PORT' followed by 4 digits */
export function validatePortfolioId(value: string): ValidationResult {
  if (!value || value.length < 8) return fail(ValidationCode.INVALID_ID);
  if (value.substring(0, 4) !== ID_PREFIX) return fail(ValidationCode.INVALID_ID);
  const digits = value.substring(4, 8);
  if (!/^\d{4}$/.test(digits)) return fail(ValidationCode.INVALID_ID);
  return ok();
}

/** 2000-VALIDATE-ACCOUNT: Account number must be 10 numeric digits and non-zero */
export function validateAccountNumber(value: string): ValidationResult {
  if (!value || !/^\d{10}$/.test(value)) return fail(ValidationCode.INVALID_ACCOUNT);
  if (value === "0000000000") return fail(ValidationCode.INVALID_ACCOUNT);
  return ok();
}

/** 3000-VALIDATE-TYPE: Investment type must be STK, BND, MMF, or ETF */
export function validateInvestmentType(value: string): ValidationResult {
  if (!VALID_INVESTMENT_TYPES.includes(value as InvestmentType)) {
    return fail(ValidationCode.INVALID_TYPE);
  }
  return ok();
}

/** 4000-VALIDATE-AMOUNT: Amount must be within valid range */
export function validateAmount(value: string | number | Decimal): ValidationResult {
  const amount = new Decimal(value);
  if (amount.lt(MIN_AMOUNT) || amount.gt(MAX_AMOUNT)) {
    return fail(ValidationCode.INVALID_AMOUNT);
  }
  return ok();
}

/** Top-level dispatcher matching PORTVALD's EVALUATE TRUE */
export type ValidationType = "I" | "A" | "T" | "M";

export function validate(
  type: ValidationType,
  value: string,
): ValidationResult {
  switch (type) {
    case "I":
      return validatePortfolioId(value);
    case "A":
      return validateAccountNumber(value);
    case "T":
      return validateInvestmentType(value);
    case "M":
      return validateAmount(value);
    default:
      return fail(ValidationCode.INVALID_ID);
  }
}
