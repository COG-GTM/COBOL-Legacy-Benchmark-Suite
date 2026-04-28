/**
 * Shared validation functions implementing rules from PORTVALD.cbl
 * Maps to: 1000-VALIDATE-ID, 2000-VALIDATE-ACCOUNT, 3000-VALIDATE-TYPE, 4000-VALIDATE-AMOUNT
 */

import type { InvestmentType } from '../types';

const VALID_INVESTMENT_TYPES: InvestmentType[] = ['STK', 'BND', 'MMF', 'ETF'];
const VAL_ID_PREFIX = 'PORT';
const VAL_MIN_AMOUNT = -9999999999999.99;
const VAL_MAX_AMOUNT = 9999999999999.99;

export interface ValidationResult {
  valid: boolean;
  error: string;
}

const ok: ValidationResult = { valid: true, error: '' };

/** 1000-VALIDATE-ID: Portfolio ID must start with 'PORT' followed by 4 numeric digits */
export function validatePortfolioId(value: string): ValidationResult {
  if (!value || value.length !== 8) {
    return { valid: false, error: 'Portfolio ID must be exactly 8 characters' };
  }
  if (value.substring(0, 4) !== VAL_ID_PREFIX) {
    return { valid: false, error: 'Invalid Portfolio ID format' };
  }
  const digits = value.substring(4, 8);
  if (!/^\d{4}$/.test(digits)) {
    return { valid: false, error: 'Invalid Portfolio ID format' };
  }
  return ok;
}

/** 2000-VALIDATE-ACCOUNT: Account number must be 10 numeric digits, non-zero */
export function validateAccountNumber(value: string): ValidationResult {
  if (!value || value.length !== 10) {
    return { valid: false, error: 'Account number must be exactly 10 digits' };
  }
  if (!/^\d{10}$/.test(value)) {
    return { valid: false, error: 'Invalid Account Number format' };
  }
  if (value === '0000000000') {
    return { valid: false, error: 'Invalid Account Number format' };
  }
  return ok;
}

/** 3000-VALIDATE-TYPE: Investment type must be STK, BND, MMF, or ETF */
export function validateInvestmentType(value: string): ValidationResult {
  if (!VALID_INVESTMENT_TYPES.includes(value as InvestmentType)) {
    return { valid: false, error: 'Invalid Investment Type' };
  }
  return ok;
}

/** 4000-VALIDATE-AMOUNT: Amount must be within valid range */
export function validateAmount(value: number): ValidationResult {
  if (isNaN(value)) {
    return { valid: false, error: 'Amount must be a valid number' };
  }
  if (value < VAL_MIN_AMOUNT || value > VAL_MAX_AMOUNT) {
    return { valid: false, error: 'Amount outside valid range' };
  }
  return ok;
}

/** Client name cannot be blank */
export function validateClientName(value: string): ValidationResult {
  if (!value || value.trim().length === 0) {
    return { valid: false, error: 'Client name cannot be blank' };
  }
  return ok;
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(value);
}

export function formatDate(yyyymmdd: string): string {
  if (yyyymmdd.length === 8) {
    return `${yyyymmdd.substring(0, 4)}-${yyyymmdd.substring(4, 6)}-${yyyymmdd.substring(6, 8)}`;
  }
  return yyyymmdd;
}

export function formatNumber(value: number, decimals = 2): string {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
}
