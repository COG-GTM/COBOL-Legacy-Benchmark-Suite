/**
 * Helpers for working with COMP-3 packed-decimal monetary values represented
 * as decimal strings. We deliberately avoid parsing these into JS numbers to
 * keep full precision for values up to `S9(13)V99` (and wider fields elsewhere
 * in the system, up to 15 digits with 2-4 decimals).
 */

const DECIMAL_RE = /^-?\d+(\.\d+)?$/;

/** Returns true if `value` is a syntactically valid signed decimal string. */
function isDecimalString(value: string): boolean {
  return DECIMAL_RE.test(value.trim());
}

/**
 * Normalizes a decimal string to a fixed number of fraction digits using
 * string operations only (no float rounding).
 */
function normalizeDecimal(value: string, fracDigits: number): string {
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

/** Inserts thousands separators into a run of integer digits. */
function groupThousands(intPart: string): string {
  return intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

/**
 * Formats a decimal string as a currency value with grouped thousands and
 * `fracDigits` decimals — 2 for TRN-AMOUNT (`S9(13)V9(2)`), 4 for the unit
 * prices carried in `S9(11)V9(4)` fields. Done purely with string
 * manipulation to preserve precision.
 */
export function formatCurrency(
  value: string,
  currency = 'USD',
  fracDigits = 2,
): string {
  if (!isDecimalString(value)) {
    return value;
  }
  const normalized = normalizeDecimal(value, fracDigits);
  const negative = normalized.startsWith('-');
  const unsigned = normalized.replace(/^-/, '');
  const [intPart, fracPart] = unsigned.split('.');
  const grouped = groupThousands(intPart);
  const symbol = currency === 'USD' ? '$' : `${currency} `;
  return `${negative ? '-' : ''}${symbol}${grouped}.${fracPart}`;
}

/**
 * Formats a quantity (`S9(11)V9(4)`) with grouped thousands, trimming trailing
 * fraction zeros for readability while never rounding. Integer-valued
 * quantities render without a decimal point (e.g. "4,200").
 */
export function formatQuantity(value: string): string {
  if (!isDecimalString(value)) {
    return value;
  }
  const normalized = normalizeDecimal(value, 4);
  const negative = normalized.startsWith('-');
  const unsigned = normalized.replace(/^-/, '');
  const [intPart, fracPart] = unsigned.split('.');
  const trimmedFrac = fracPart.replace(/0+$/, '');
  const grouped = groupThousands(intPart);
  const body = trimmedFrac ? `${grouped}.${trimmedFrac}` : grouped;
  return negative ? `-${body}` : body;
}
