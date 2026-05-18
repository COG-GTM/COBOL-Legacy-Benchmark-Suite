export interface ValidationResult {
  valid: boolean;
  error?: string;
}

export function validateAccountNumber(value: string): ValidationResult {
  if (!/^\d{9}$/.test(value)) {
    return { valid: false, error: 'Account number must be exactly 9 numeric digits' };
  }
  const num = parseInt(value, 10);
  if (num < 100000000 || num > 999999999) {
    return { valid: false, error: 'Account number must be between 100000000 and 999999999' };
  }
  return { valid: true };
}

export function validateFundId(value: string): ValidationResult {
  if (!/^[A-Za-z0-9]{1,6}$/.test(value)) {
    return { valid: false, error: 'Fund ID must be 1-6 alphanumeric characters' };
  }
  return { valid: true };
}

export function validatePortfolioId(value: string): ValidationResult {
  if (!/^PORT\d{5}$/.test(value)) {
    return { valid: false, error: 'Portfolio ID must start with "PORT" followed by 5 numeric digits' };
  }
  return { valid: true };
}

export function validatePortfolioName(value: string): ValidationResult {
  if (!value || value.trim().length === 0) {
    return { valid: false, error: 'Portfolio name is required' };
  }
  return { valid: true };
}

export function validatePortfolioStatus(value: string): ValidationResult {
  if (!['A', 'I', 'C'].includes(value)) {
    return { valid: false, error: 'Status must be Active (A), Inactive (I), or Closed (C)' };
  }
  return { valid: true };
}

export function validateShareQuantity(value: number, transType: string): ValidationResult {
  if ((transType === 'BY' || transType === 'SL') && value === 0) {
    return { valid: false, error: 'Share quantity must be non-zero for Buy/Sell transactions' };
  }
  return { valid: true };
}

export function validatePrice(value: number, transType: string): ValidationResult {
  if ((transType === 'BY' || transType === 'SL') && value <= 0) {
    return { valid: false, error: 'Price must be greater than zero for Buy/Sell transactions' };
  }
  return { valid: true };
}

export function validateAmount(value: number, transType: string): ValidationResult {
  if (transType === 'FE' && value === 0) {
    return { valid: false, error: 'Amount must be non-zero for Fee transactions' };
  }
  return { valid: true };
}

export function validateTransactionDate(value: string): ValidationResult {
  const date = new Date(value);
  if (isNaN(date.getTime())) {
    return { valid: false, error: 'Invalid date format' };
  }
  const today = new Date();
  today.setHours(23, 59, 59, 999);
  if (date > today) {
    return { valid: false, error: 'Transaction date cannot be in the future' };
  }
  return { valid: true };
}

export function validateTransactionType(value: string): ValidationResult {
  if (!['BY', 'SL', 'FE'].includes(value)) {
    return { valid: false, error: 'Transaction type must be Buy (BY), Sell (SL), or Fee (FE)' };
  }
  return { valid: true };
}
