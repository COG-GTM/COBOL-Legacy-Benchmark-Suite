/**
 * Portfolio validation utilities
 * Modernized from COBOL PORTVALD subroutine
 */

export interface ValidationResult {
  valid: boolean;
  error: string | null;
}

/**
 * Validates a Portfolio ID.
 * Must match pattern: PORT + exactly 4 numeric digits (e.g., PORT0001)
 */
export function validatePortfolioId(id: string): ValidationResult {
  const trimmed = id.trim();
  if (!trimmed) {
    return { valid: false, error: 'Portfolio ID is required' };
  }
  const pattern = /^PORT\d{4}$/;
  if (!pattern.test(trimmed)) {
    return { valid: false, error: 'Portfolio ID must start with "PORT" followed by exactly 4 digits (e.g., PORT0001)' };
  }
  return { valid: true, error: null };
}

/**
 * Validates an Account Number.
 * Must be exactly 10 numeric digits and non-zero.
 */
export function validateAccountNumber(accountNo: string): ValidationResult {
  const trimmed = accountNo.trim();
  if (!trimmed) {
    return { valid: false, error: 'Account number is required' };
  }
  const pattern = /^\d{10}$/;
  if (!pattern.test(trimmed)) {
    return { valid: false, error: 'Account number must be exactly 10 numeric digits' };
  }
  if (trimmed === '0000000000') {
    return { valid: false, error: 'Account number cannot be all zeros' };
  }
  return { valid: true, error: null };
}

/**
 * Validates an Investment Type.
 * Must be one of: STK, BND, MMF, ETF
 */
export function validateInvestmentType(type: string): ValidationResult {
  const validTypes = ['STK', 'BND', 'MMF', 'ETF'];
  const trimmed = type.trim().toUpperCase();
  if (!trimmed) {
    return { valid: false, error: 'Investment type is required' };
  }
  if (!validTypes.includes(trimmed)) {
    return { valid: false, error: 'Investment type must be one of: STK, BND, MMF, ETF' };
  }
  return { valid: true, error: null };
}

/**
 * Validates a monetary amount.
 * Must be within range: 0 to 99,999,999.99
 */
export function validateAmount(amount: number): ValidationResult {
  if (isNaN(amount)) {
    return { valid: false, error: 'Amount must be a valid number' };
  }
  if (amount < 0 || amount > 99999999.99) {
    return { valid: false, error: 'Amount must be between $0.00 and $99,999,999.99' };
  }
  return { valid: true, error: null };
}

/**
 * Validates a Portfolio Name.
 * Must not be empty or only whitespace.
 */
export function validatePortfolioName(name: string): ValidationResult {
  const trimmed = name.trim();
  if (!trimmed) {
    return { valid: false, error: 'Portfolio name is required' };
  }
  return { valid: true, error: null };
}

/**
 * Validates a Portfolio Status.
 * Must be A (Active), I (Inactive), or C (Closed).
 */
export function validatePortfolioStatus(status: string): ValidationResult {
  const validStatuses = ['A', 'I', 'C'];
  if (!validStatuses.includes(status)) {
    return { valid: false, error: 'Status must be Active, Inactive, or Closed' };
  }
  return { valid: true, error: null };
}
