import { describe, expect, it } from 'vitest';
import { formatIsoDate, formatTime, formatTimestamp } from './date';
import { formatDecimal, formatHistoryQuantity, formatMoney, formatPositionUnits } from './decimal';
import { positionStatusLabel, transactionTypeLabel } from './labels';

describe('decimal formatting (FR-011, FR-019, FR-032, NFR-04)', () => {
  it('formats position units to 4 dp', () => {
    expect(formatPositionUnits('65.789500')).toBe('65.7895');
    expect(formatPositionUnits('120')).toBe('120.0000');
  });

  it('formats money to 2 dp with currency symbol and thousands separators', () => {
    expect(formatMoney('12500.00', 'USD')).toBe('$12,500.00');
    expect(formatMoney('118765.43')).toBe('$118,765.43');
  });

  it('formats negative amounts with a sign', () => {
    expect(formatMoney('-25.00')).toBe('-$25.00');
  });

  it('formats history units and prices to 3 dp', () => {
    expect(formatHistoryQuantity('65.7895')).toBe('65.790');
    expect(formatHistoryQuantity('-10.0000')).toBe('-10.000');
    expect(formatHistoryQuantity('190.0000')).toBe('190.000');
  });

  it('rounds half-up on the decimal string without float drift', () => {
    expect(formatDecimal('1.005', { decimals: 2 })).toBe('1.01');
    expect(formatDecimal('0.0005', { decimals: 3 })).toBe('0.001');
    expect(formatDecimal('9.9995', { decimals: 3 })).toBe('10.000');
  });

  it('preserves precision for the largest DECIMAL(15,2) value (FR-032)', () => {
    expect(formatMoney('9999999999999.99')).toBe('$9,999,999,999,999.99');
    expect(formatPositionUnits('99999999999.9999')).toBe('99,999,999,999.9999');
  });

  it('shows the raw value for malformed input instead of NaN', () => {
    expect(formatMoney('abc')).toBe('abc');
  });
});

describe('date formatting (FR-010, FR-019)', () => {
  it('keeps ISO dates as YYYY-MM-DD and converts legacy YYYYMMDD', () => {
    expect(formatIsoDate('2024-03-20')).toBe('2024-03-20');
    expect(formatIsoDate('20240320')).toBe('2024-03-20');
  });

  it('formats times as HH:MM:SS', () => {
    expect(formatTime('14:30:00')).toBe('14:30:00');
  });

  it('formats RFC 3339 timestamps in UTC', () => {
    expect(formatTimestamp('2024-03-20T15:31:12Z')).toBe('2024-03-20 15:31:12 UTC');
  });
});

describe('code labels (FR-016, FR-019)', () => {
  it('maps transaction types to code + label and leaves unknown codes raw', () => {
    expect(transactionTypeLabel('BU')).toBe('BU – Buy');
    expect(transactionTypeLabel('SL')).toBe('SL – Sell');
    expect(transactionTypeLabel('TR')).toBe('TR – Transfer');
    expect(transactionTypeLabel('FE')).toBe('FE – Fee');
    expect(transactionTypeLabel('XX')).toBe('XX');
  });

  it('maps position statuses', () => {
    expect(positionStatusLabel('A')).toBe('Active');
    expect(positionStatusLabel('C')).toBe('Closed');
    expect(positionStatusLabel('P')).toBe('Pending');
  });
});
