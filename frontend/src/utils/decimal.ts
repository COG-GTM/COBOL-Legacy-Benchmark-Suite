/**
 * Helpers for working with COMP-3 packed-decimal monetary values represented
 * as decimal strings. We deliberately avoid parsing these into JS numbers to
 * keep full precision for values up to `S9(13)V99` (and wider fields elsewhere
 * in the system, up to 15 digits with 2-4 decimals).
 */

const DECIMAL_RE = /^-?\d+(\.\d+)?$/;

export interface DecimalConstraints {
  /** Max number of digits to the left of the decimal point. */
  maxIntDigits: number;
  /** Max number of fraction digits. */
  maxFracDigits: number;
}

/** Returns true if `value` is a syntactically valid signed decimal string. */
export function isDecimalString(value: string): boolean {
  return DECIMAL_RE.test(value.trim());
}

/**
 * Validates a decimal string against PIC-style constraints without converting
 * to a (lossy) number. Returns an error message, or null when valid.
 */
export function validateDecimal(
  value: string,
  { maxIntDigits, maxFracDigits }: DecimalConstraints,
): string | null {
  const trimmed = value.trim();
  if (!isDecimalString(trimmed)) {
    return 'Enter a valid number (e.g. 1234.56).';
  }
  const unsigned = trimmed.replace(/^-/, '');
  const [intPart, fracPart = ''] = unsigned.split('.');
  if (intPart.replace(/^0+(?=\d)/, '').length > maxIntDigits) {
    return `Up to ${maxIntDigits} digits before the decimal point.`;
  }
  if (fracPart.length > maxFracDigits) {
    return `Up to ${maxFracDigits} decimal places.`;
  }
  return null;
}

/**
 * Normalizes a decimal string to a fixed number of fraction digits using
 * string operations only (no float rounding). Assumes `value` already passed
 * {@link validateDecimal}.
 */
export function normalizeDecimal(value: string, fracDigits = 2): string {
  const trimmed = value.trim();
  const negative = trimmed.startsWith('-');
  const unsigned = trimmed.replace(/^-/, '');
  const [rawInt, rawFrac = ''] = unsigned.split('.');
  const intPart = rawInt.replace(/^0+(?=\d)/, '') || '0';
  const fracPart = rawFrac.slice(0, fracDigits).padEnd(fracDigits, '0');
  const body = fracDigits > 0 ? `${intPart}.${fracPart}` : intPart;
  const isZero = /^0(\.0+)?$/.test(body);
  return negative && !isZero ? `-${body}` : body;
}

/**
 * Formats a decimal string as a currency value with grouped thousands.
 * Done purely with string manipulation to preserve precision.
 */
export function formatCurrency(value: string, currency = 'USD'): string {
  if (!isDecimalString(value)) {
    return value;
  }
  const normalized = normalizeDecimal(value, 2);
  const negative = normalized.startsWith('-');
  const unsigned = normalized.replace(/^-/, '');
  const [intPart, fracPart] = unsigned.split('.');
  const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const symbol = currency === 'USD' ? '$' : `${currency} `;
  return `${negative ? '-' : ''}${symbol}${grouped}.${fracPart}`;
}
