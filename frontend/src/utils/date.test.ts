import { describe, expect, it } from 'vitest';
import { formatCobolDate, formatCobolTime, toCobolDate } from './date';

describe('COBOL date/time helpers', () => {
  it('formats HIST-DATE and HIST-TIME', () => {
    expect(formatCobolDate('20240408')).toBe('2024-04-08');
    expect(formatCobolTime('091503')).toBe('09:15:03');
  });

  it('passes malformed values through, falling back to an em dash', () => {
    expect(formatCobolDate('2024')).toBe('2024');
    expect(formatCobolDate('')).toBe('—');
    expect(formatCobolTime('')).toBe('—');
  });

  it('converts date-input values to YYYYMMDD, ignoring blanks', () => {
    expect(toCobolDate('2024-03-01')).toBe('20240301');
    expect(toCobolDate('')).toBe('');
  });
});
