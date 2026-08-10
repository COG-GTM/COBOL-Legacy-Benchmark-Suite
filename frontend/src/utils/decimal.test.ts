import { describe, expect, it } from 'vitest';
import { formatCurrency, formatQuantity } from './decimal';

describe('formatCurrency', () => {
  it('groups thousands and keeps the requested precision', () => {
    expect(formatCurrency('1234567.89')).toBe('$1,234,567.89');
    expect(formatCurrency('151.0492', 'USD', 4)).toBe('$151.0492');
    expect(formatCurrency('1000', 'EUR')).toBe('EUR 1,000.00');
  });

  it('keeps precision beyond the JS safe-integer range', () => {
    expect(formatCurrency('9999999999999.99')).toBe('$9,999,999,999,999.99');
  });

  it('never rounds: extra fraction digits are truncated', () => {
    expect(formatCurrency('1.999')).toBe('$1.99');
  });

  it('returns non-numeric input unchanged', () => {
    expect(formatCurrency('—')).toBe('—');
  });
});

describe('formatQuantity', () => {
  it('trims trailing fraction zeros', () => {
    expect(formatQuantity('4200.0000')).toBe('4,200');
    expect(formatQuantity('3251.7050')).toBe('3,251.705');
    expect(formatQuantity('-12.5000')).toBe('-12.5');
  });
});
