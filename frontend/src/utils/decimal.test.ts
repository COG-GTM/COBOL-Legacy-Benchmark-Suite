import { describe, expect, it } from 'vitest';
import {
  addDecimals,
  compareDecimals,
  formatCurrency,
  formatQuantity,
  multiplyDecimals,
  normalizeDecimal,
  subtractDecimals,
  sumDecimals,
  validateDecimal,
} from './decimal';

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

describe('formatQuantity', () => {
  it('groups thousands and trims trailing fraction zeros', () => {
    expect(formatQuantity('4200.0000')).toBe('4,200');
    expect(formatQuantity('1250.5000')).toBe('1,250.5');
    expect(formatQuantity('1975.3300')).toBe('1,975.33');
    expect(formatQuantity('157820.4000')).toBe('157,820.4');
  });

  it('preserves up to four fraction digits and handles negatives', () => {
    expect(formatQuantity('0.0001')).toBe('0.0001');
    expect(formatQuantity('-12.5000')).toBe('-12.5');
  });
});

describe('decimal arithmetic', () => {
  it('adds without floating-point error', () => {
    expect(addDecimals('0.1', '0.2')).toBe('0.30');
    expect(addDecimals('512300.75', '172480.20')).toBe('684780.95');
  });

  it('subtracts, including negative results', () => {
    expect(subtractDecimals('684780.95', '579050.00')).toBe('105730.95');
    expect(subtractDecimals('58940.10', '61200.00')).toBe('-2259.90');
  });

  it('sums a list preserving precision on large values', () => {
    expect(sumDecimals(['0.01', '0.02', '0.03'])).toBe('0.06');
    expect(
      sumDecimals(['123456789012345.67', '1.11', '0.22']),
    ).toBe('123456789012347.00');
  });

  it('returns 0.00 for an empty sum', () => {
    expect(sumDecimals([])).toBe('0.00');
  });

  it('multiplies with truncation to the target scale', () => {
    expect(multiplyDecimals('250', '409.82')).toBe('102455.00');
    expect(multiplyDecimals('1.9999', '1.9999')).toBe('3.99');
    expect(multiplyDecimals('-2.5', '4')).toBe('-10.00');
  });

  it('compares without converting to a lossy number', () => {
    expect(compareDecimals('1250.5000', '1250.5')).toBe(0);
    expect(compareDecimals('99.9999', '100')).toBeLessThan(0);
    expect(
      compareDecimals('123456789012345.68', '123456789012345.67', 2),
    ).toBeGreaterThan(0);
  });
});
