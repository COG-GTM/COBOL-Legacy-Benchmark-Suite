/** Formatting helpers for COBOL-derived values. */

/** Format a decimal number as USD currency with 2 fraction digits. */
export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/** Format unit quantities (POS-QUANTITY has 4 implied decimals). */
export function formatUnits(value: number, fractionDigits = 4): string {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: fractionDigits,
  }).format(value);
}

/** Convert a COBOL YYYYMMDD date (PIC 9(8)/X(8)) to YYYY-MM-DD for display. */
export function formatCobolDate(yyyymmdd: string): string {
  const trimmed = yyyymmdd.trim();
  if (/^\d{8}$/.test(trimmed)) {
    return `${trimmed.slice(0, 4)}-${trimmed.slice(4, 6)}-${trimmed.slice(6, 8)}`;
  }
  return trimmed;
}
