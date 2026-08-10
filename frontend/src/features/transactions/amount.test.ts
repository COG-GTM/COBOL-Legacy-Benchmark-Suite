import { describe, expect, it } from 'vitest';
import { calculateAmount } from './amount';

describe('calculateAmount', () => {
  it('multiplies quantity by price into a V9(2) amount', () => {
    expect(calculateAmount('BU', '250', '409.82')).toBe('102455.00');
    expect(calculateAmount('SL', '40.5', '198.75')).toBe('8049.37');
  });

  it('truncates rather than rounds, as an unqualified COMPUTE does', () => {
    // 325.5 x 61.7333 = 20094.18915 -> 20094.18 (rounding would give .19)
    expect(calculateAmount('BU', '325.5', '61.7333')).toBe('20094.18');
  });

  it('keeps full precision beyond the double safe-integer range', () => {
    expect(calculateAmount('BU', '99999999999', '9.9999')).toBe(
      '999989999990.00',
    );
  });

  it('returns a zero amount for transfers, which carry no price', () => {
    expect(calculateAmount('TR', '500', '')).toBe('0.00');
  });

  it('returns an empty string while an operand is blank or malformed', () => {
    expect(calculateAmount('BU', '', '10')).toBe('');
    expect(calculateAmount('BU', '10', 'abc')).toBe('');
  });
});
