import type { DecimalString } from '../api/types';

const DECIMAL_RE = /^([+-])?(\d+)(?:\.(\d*))?$/;
export const DEFAULT_LOCALE = 'en-US';

interface ParsedDecimal {
  negative: boolean;
  integer: string;
  fraction: string;
}

/** Rounds a non-negative digit string pair to `decimals` places, half-up, using string arithmetic only. */
function roundDigits(integer: string, fraction: string, decimals: number): { integer: string; fraction: string } {
  if (fraction.length <= decimals) return { integer, fraction: fraction.padEnd(decimals, '0') };
  const keep = fraction.slice(0, decimals);
  const roundUp = Number(fraction.charAt(decimals)) >= 5;
  if (!roundUp) return { integer, fraction: keep };
  const digits = (integer + keep).split('').map(Number);
  let i = digits.length - 1;
  for (; i >= 0; i--) {
    if (digits[i] === 9) {
      digits[i] = 0;
    } else {
      digits[i] = (digits[i] ?? 0) + 1;
      break;
    }
  }
  if (i < 0) digits.unshift(1);
  const joined = digits.join('');
  const intLen = joined.length - decimals;
  return { integer: joined.slice(0, intLen), fraction: joined.slice(intLen) };
}

export function parseDecimal(value: DecimalString, decimals: number): ParsedDecimal | null {
  const match = DECIMAL_RE.exec(value.trim());
  if (!match) return null;
  const rounded = roundDigits(match[2] ?? '0', match[3] ?? '', decimals);
  const integer = rounded.integer.replace(/^0+(?=\d)/, '');
  const isZero = /^0*$/.test(integer) && /^0*$/.test(rounded.fraction);
  return { negative: match[1] === '-' && !isZero, integer, fraction: rounded.fraction };
}

export interface FormatDecimalOptions {
  decimals: number;
  currency?: string;
  locale?: string;
}

/**
 * Formats a decimal string without ever converting it to a JS number (BR-09, FR-032, NFR-04).
 * Locale symbols, grouping and sign placement come from Intl; the digits come from the string.
 * Invalid input is returned unchanged so bad data is visible rather than silently rewritten.
 */
export function formatDecimal(value: DecimalString | null | undefined, options: FormatDecimalOptions): string {
  if (value === null || value === undefined) return '';
  const { decimals, currency, locale = DEFAULT_LOCALE } = options;
  const parsed = parseDecimal(value, decimals);
  if (!parsed) return value;

  const template = new Intl.NumberFormat(locale, {
    style: currency ? 'currency' : 'decimal',
    currency,
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).formatToParts(parsed.negative ? -1 : 1);

  const groupedInteger = new Intl.NumberFormat(locale, { useGrouping: true, maximumFractionDigits: 0 }).format(
    BigInt(parsed.integer),
  );

  return template
    .map((part) => {
      if (part.type === 'integer') return groupedInteger;
      if (part.type === 'fraction') return parsed.fraction;
      return part.value;
    })
    .join('');
}

/** Money: 2 decimals, currency symbol, signed (FR-011, FR-019). */
export const formatMoney = (value: DecimalString | null | undefined, currency = 'USD', locale?: string) =>
  formatDecimal(value, { decimals: 2, currency: currency || 'USD', locale });

/** Position units: 4 decimals (POS-QUANTITY S9(11)V9(4)). */
export const formatPositionUnits = (value: DecimalString | null | undefined, locale?: string) =>
  formatDecimal(value, { decimals: 4, locale });

/** History units / price: 3 decimals (POSHIST DECIMAL(15,3)). */
export const formatHistoryQuantity = (value: DecimalString | null | undefined, locale?: string) =>
  formatDecimal(value, { decimals: 3, locale });
