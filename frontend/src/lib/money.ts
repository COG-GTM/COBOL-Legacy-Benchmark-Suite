/** Converts a V99 amount to exact integer cents. */
export function toCents(value: number): bigint {
  return BigInt(Math.round(value * 100))
}

/** Sum V99 amounts as bigint cents so totals neither drift nor lose precision. */
export function sumCents(values: number[]): bigint {
  return values.reduce((total, value) => total + toCents(value), 0n)
}
