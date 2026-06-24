import { describe, expect, it } from 'vitest';
import { formatCurrency, normalizeDecimal, validateDecimal } from './decimal';

describe('validateDecimal', () => {
  const constraints = { maxIntDigits: 13, maxFracDigits: 2 };

  it('accepts valid signed decimals', () => {
    expect(validateDecimal('1234.56', constraints)).toBeNull();
    expect(validateDecimal('-1234.56', constraints)).toBeNull();
    expect(validateDecimal('0', constraints)).toBeNull();
    expect(validateDecimal('1234567890123.99', constraints)).toBeNull();
  });

  it('rejects non-numeric input', () => {
    expect(validateDecimal('abc', constraints)).not.toBeNull();
    expect(validateDecimal('1,234.56', constraints)).not.toBeNull();
    expect(validateDecimal('', constraints)).not.toBeNull();
  });

  it('enforces integer and fraction digit limits', () => {
    expect(validateDecimal('12345678901234', constraints)).toMatch(
      /13 digits/,
    );
    expect(validateDecimal('1.234', constraints)).toMatch(/2 decimal/);
  });
});

describe('normalizeDecimal', () => {
  it('pads to two fraction digits without float rounding', () => {
    expect(normalizeDecimal('1234.5')).toBe('1234.50');
    expect(normalizeDecimal('1234')).toBe('1234.00');
    expect(normalizeDecimal('-1234.5')).toBe('-1234.50');
  });

  it('strips leading zeros and normalizes negative zero', () => {
    expect(normalizeDecimal('007.10')).toBe('7.10');
    expect(normalizeDecimal('-0.00')).toBe('0.00');
  });
});

describe('formatCurrency', () => {
  it('groups thousands and preserves precision for large values', () => {
    expect(formatCurrency('12503488.99')).toBe('$12,503,488.99');
    expect(formatCurrency('0.00')).toBe('$0.00');
    expect(formatCurrency('-1234.5')).toBe('-$1,234.50');
  });

  it('does not lose precision on 15-digit values', () => {
    expect(formatCurrency('123456789012345.67')).toBe(
      '$123,456,789,012,345.67',
    );
  });
});
