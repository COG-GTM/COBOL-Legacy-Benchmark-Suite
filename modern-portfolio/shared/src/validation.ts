// Portfolio validation logic from PORTMSTR.cbl lines 138-161

const PORTFOLIO_ID_REGEX = /^PORT\d{5}$/;

export function validatePortfolioId(id: string): { valid: boolean; error?: string } {
  if (!PORTFOLIO_ID_REGEX.test(id)) {
    return {
      valid: false,
      error: "Invalid Portfolio ID format. Must be 'PORT' followed by 5 numeric digits (e.g., PORT12345)",
    };
  }
  return { valid: true };
}

export function validateRequired(value: string | undefined | null, fieldName: string): { valid: boolean; error?: string } {
  if (!value || value.trim().length === 0) {
    return { valid: false, error: `${fieldName} is required` };
  }
  return { valid: true };
}

export function validateTransactionType(type: string): { valid: boolean; error?: string } {
  const validTypes = ['BU', 'SL', 'TR', 'FE'];
  if (!validTypes.includes(type)) {
    return {
      valid: false,
      error: `Invalid transaction type '${type}'. Must be one of: BU (Buy), SL (Sell), TR (Transfer), FE (Fee)`,
    };
  }
  return { valid: true };
}

export function validateStatusTransition(
  currentStatus: string,
  newStatus: string,
  validTransitions: Record<string, string[]>
): { valid: boolean; error?: string } {
  const allowed = validTransitions[currentStatus];
  if (!allowed || !allowed.includes(newStatus)) {
    return {
      valid: false,
      error: `Invalid status transition from '${currentStatus}' to '${newStatus}'`,
    };
  }
  return { valid: true };
}

export const PORTFOLIO_STATUS_TRANSITIONS: Record<string, string[]> = {
  A: ['C', 'S'],
  S: ['A', 'C'],
  C: [],
};

export const TRANSACTION_STATUS_TRANSITIONS: Record<string, string[]> = {
  P: ['D', 'F', 'R'],
  D: ['R'],
  F: [],
  R: [],
};
