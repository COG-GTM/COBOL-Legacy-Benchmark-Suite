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

/** Inserts thousands separators into a run of integer digits. */
function groupThousands(intPart: string): string {
  return intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
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
  const grouped = groupThousands(intPart);
  const symbol = currency === 'USD' ? '$' : `${currency} `;
  return `${negative ? '-' : ''}${symbol}${grouped}.${fracPart}`;
}

/**
 * Formats a POS-QUANTITY (`S9(11)V9(4)`) with grouped thousands, trimming
 * trailing fraction zeros for readability while never rounding. Integer-valued
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

/**
 * Converts a validated decimal string into an integer scaled by `scale`
 * fraction digits. Uses BigInt so no precision is lost regardless of magnitude.
 */
function toScaledInt(value: string, scale: number): bigint {
  const normalized = normalizeDecimal(value, scale);
  const negative = normalized.startsWith('-');
  const digits = normalized.replace(/^-/, '').replace('.', '');
  const magnitude = BigInt(digits);
  return negative ? -magnitude : magnitude;
}

/** Renders a scaled BigInt back into a decimal string with `scale` fractions. */
function fromScaledInt(scaled: bigint, scale: number): string {
  const negative = scaled < 0n;
  const digits = (negative ? -scaled : scaled)
    .toString()
    .padStart(scale + 1, '0');
  const intPart = digits.slice(0, digits.length - scale);
  const fracPart = scale > 0 ? digits.slice(digits.length - scale) : '';
  const body = scale > 0 ? `${intPart}.${fracPart}` : intPart;
  const isZero = /^0(\.0+)?$/.test(body);
  return negative && !isZero ? `-${body}` : body;
}

/**
 * Adds two decimal strings without floating-point error. The result keeps
 * `scale` fraction digits (default 2, for `S9(13)V9(2)` money fields).
 */
export function addDecimals(a: string, b: string, scale = 2): string {
  return fromScaledInt(toScaledInt(a, scale) + toScaledInt(b, scale), scale);
}

/** Subtracts `b` from `a` as decimal strings, preserving precision. */
export function subtractDecimals(a: string, b: string, scale = 2): string {
  return fromScaledInt(toScaledInt(a, scale) - toScaledInt(b, scale), scale);
}

/**
 * Multiplies two decimal strings, keeping `scale` fraction digits in the
 * result. Extra digits are truncated rather than rounded, matching an
 * unqualified COBOL `COMPUTE` into a `V9(2)` field (no `ROUNDED` phrase).
 *
 * `aScale` / `bScale` are the fraction digits of the operands — 4 by default
 * for the `S9(11)V9(4)` quantity and price fields.
 */
export function multiplyDecimals(
  a: string,
  b: string,
  { scale = 2, aScale = 4, bScale = 4 } = {},
): string {
  const product = toScaledInt(a, aScale) * toScaledInt(b, bScale);
  const excess = aScale + bScale - scale;
  const divisor = 10n ** BigInt(Math.max(excess, 0));
  const scaled =
    excess >= 0 ? product / divisor : product * 10n ** BigInt(-excess);
  return fromScaledInt(scaled, scale);
}

/**
 * Compares two decimal strings, returning a negative number when `a < b`,
 * zero when equal and a positive number when `a > b`.
 */
export function compareDecimals(a: string, b: string, scale = 4): number {
  const left = toScaledInt(a, scale);
  const right = toScaledInt(b, scale);
  if (left === right) return 0;
  return left < right ? -1 : 1;
}

/** Sums a list of decimal strings, preserving precision. */
export function sumDecimals(values: readonly string[], scale = 2): string {
  const total = values.reduce(
    (acc, value) => acc + toScaledInt(value, scale),
    0n,
  );
  return fromScaledInt(total, scale);
}
