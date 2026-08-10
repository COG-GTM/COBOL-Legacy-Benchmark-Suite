import { describe, expect, it } from 'vitest';
import {
  cobolTimestampDate,
  formatCobolDate,
  formatCobolTimestamp,
  inclusiveDayCount,
  shiftCobolDate,
  toIsoDate,
} from './date';

describe('COBOL date helpers', () => {
  it('formats PIC 9(8) dates and falls back on malformed input', () => {
    expect(formatCobolDate('20240401')).toBe('2024-04-01');
    expect(formatCobolDate('')).toBe('—');
    expect(toIsoDate('20240401')).toBe('2024-04-01');
    expect(toIsoDate('')).toBe('');
    expect(toIsoDate('2024-04-01')).toBe('');
  });

  it('reads the date and time out of a PIC X(26) timestamp', () => {
    expect(cobolTimestampDate('2024-04-01-09.31.22.845000')).toBe('20240401');
    expect(formatCobolTimestamp('2024-04-01-09.31.22.845000')).toBe(
      '2024-04-01 09:31:22',
    );
  });

  it('shifts dates across month and year boundaries', () => {
    expect(shiftCobolDate('20240401', -1)).toBe('20240331');
    expect(shiftCobolDate('20240228', 2)).toBe('20240301');
    expect(shiftCobolDate('20241231', 1)).toBe('20250101');
    expect(shiftCobolDate('bad', 1)).toBe('bad');
  });

  it('counts a date range inclusively', () => {
    expect(inclusiveDayCount('20240401', '20240401')).toBe(1);
    expect(inclusiveDayCount('20240329', '20240402')).toBe(5);
    expect(inclusiveDayCount('20240402', '20240401')).toBe(1);
  });
});
