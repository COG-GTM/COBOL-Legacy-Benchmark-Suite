import { describe, expect, it } from 'vitest';
import { normaliseAccountNo, parsePageParam, validateAccountNo } from './validation';

const INVALID = 'Invalid Account Number format';

describe('account validation (FR-007)', () => {
  it.each(['1000000001', '1', '42', ' 1000000001 '])('accepts %j', (value) => {
    expect(validateAccountNo(value)).toBeNull();
  });

  it.each(['', '   ', '0000000000', '0', '12345678901', '12a', '-1', '1.5', '１２３'])('rejects %j', (value) => {
    expect(validateAccountNo(value)).toBe(INVALID);
  });

  it('trims surrounding whitespace', () => {
    expect(normaliseAccountNo('  1000000001 ')).toBe('1000000001');
  });
});

describe('page param (FR-025)', () => {
  it('defaults invalid or missing pages to 1', () => {
    expect(parsePageParam(null)).toBe(1);
    expect(parsePageParam('0')).toBe(1);
    expect(parsePageParam('-3')).toBe(1);
    expect(parsePageParam('abc')).toBe(1);
    expect(parsePageParam('2')).toBe(2);
  });
});
