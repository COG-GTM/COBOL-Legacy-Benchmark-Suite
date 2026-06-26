/**
 * Display formatting helpers. The legacy COMP-3 fields are fixed-point decimals;
 * we format with fixed precision so values render like the 3270 output fields
 * (no floating-point surprises in the UI).
 */

const moneyFmt = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const unitsFmt = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});

/** Format a monetary amount (cost basis, market value, price, amount). */
export function formatMoney(value: number): string {
  return moneyFmt.format(value);
}

/** Format a holding quantity (POS-QUANTITY / WS-TRANS-UNITS). */
export function formatUnits(value: number): string {
  return unitsFmt.format(value);
}
