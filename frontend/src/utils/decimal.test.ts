import { describe, expect, it } from 'vitest';
import { addDecimals, formatCurrency } from './decimal';

describe('addDecimals', () => {
  it('adds aligned two-decimal values exactly', () => {
    expect(addDecimals('1284530.75', '8845200.00')).toBe('10129730.75');
  });

  it('starts from a zero accumulator', () => {
    expect(addDecimals('0.00', '157820.40')).toBe('157820.40');
  });

  it('preserves precision beyond the IEEE-754 safe range', () => {
    expect(addDecimals('99999999999.99', '0.02')).toBe('100000000000.01');
  });

  it('handles differing fraction widths', () => {
    expect(addDecimals('1.5', '2.25')).toBe('3.75');
  });

  it('handles negative operands', () => {
    expect(addDecimals('100.00', '-30.50')).toBe('69.50');
  });
});

describe('formatCurrency', () => {
  it('groups thousands and keeps two decimals', () => {
    expect(formatCurrency('12519846.30')).toBe('$12,519,846.30');
  });
});
