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

/** Sums a list of decimal strings, preserving precision. */
export function sumDecimals(values: readonly string[], scale = 2): string {
  const total = values.reduce(
    (acc, value) => acc + toScaledInt(value, scale),
    0n,
  );
  return fromScaledInt(total, scale);
}

/**
 * Working precision for division. Operands are read at this many fraction
 * digits before the quotient is rounded down to the caller's `scale`, so
 * inputs carrying up to 4 decimals (POS-QUANTITY) divide without truncation.
 */
const DIVISION_SCALE = 6;

/**
 * Divides two decimal strings, rounding the quotient half-away-from-zero to
 * `scale` fraction digits. Returns null when the divisor is zero, which lets
 * callers render the COBOL convention of leaving a rate blank rather than
 * reporting a misleading 0.00 (the batch reports simply skip the COMPUTE).
 */
export function divideDecimals(a: string, b: string, scale = 2): string | null {
  const numerator = toScaledInt(a, DIVISION_SCALE);
  const denominator = toScaledInt(b, DIVISION_SCALE);
  if (denominator === 0n) {
    return null;
  }
  const negative = numerator < 0n !== denominator < 0n;
  const absNumerator = numerator < 0n ? -numerator : numerator;
  const absDenominator = denominator < 0n ? -denominator : denominator;
  const scaled =
    (absNumerator * 10n ** BigInt(scale) * 2n + absDenominator) /
    (absDenominator * 2n);
  return fromScaledInt(negative ? -scaled : scaled, scale);
}

/**
 * `part / whole × 100`, e.g. the WS-SUCCESS-RATE and WS-POS-CHANGE-PCT
 * computations in RPTSTA00 / RPTPOS00. Returns null when `whole` is zero.
 */
export function percentageOf(
  part: string,
  whole: string,
  scale = 2,
): string | null {
  return divideDecimals(shiftDecimalPoint(part, 2), whole, scale);
}

/**
 * Shifts the decimal point of a decimal string right by `power` digits, i.e.
 * multiplies by 10^power without touching floating point.
 */
export function shiftDecimalPoint(value: string, power: number): string {
  const scaled = toScaledInt(value, DIVISION_SCALE) * 10n ** BigInt(power);
  return fromScaledInt(scaled, DIVISION_SCALE);
}

/**
 * Renders a percentage decimal string for display. A null value (undefined
 * rate) renders as an em dash; `signed` prefixes non-negative values with '+'
 * to mirror the `+ZZ9.99` edited picture used for change columns.
 */
export function formatPercent(
  value: string | null,
  { signed = false }: { signed?: boolean } = {},
): string {
  if (value === null || !isDecimalString(value)) {
    return '—';
  }
  const normalized = normalizeDecimal(value, 2);
  const sign = signed && !normalized.startsWith('-') ? '+' : '';
  return `${sign}${normalized}%`;
}
