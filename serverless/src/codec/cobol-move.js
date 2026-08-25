import { scaledToDecimalString } from './comp3.js';

/**
 * Models MOVE from an all-digit alphanumeric X(50) value into
 * PIC S9(13)V99: the rightmost 13 digits become the integer part and
 * the implied decimal places are zero. Non-digit input is rejected because
 * this suite intentionally does not guess GnuCOBOL's handling of junk.
 */
export function moveAlphanumericToScaled(value, { integerDigits = 13, scale = 2 } = {}) {
  const text = String(value ?? '');
  if (!/^\d+$/.test(text)) {
    throw new Error(`COBOL numeric MOVE requires digits only: ${JSON.stringify(value)}`);
  }
  const integer = text.slice(-integerDigits).padStart(integerDigits, '0');
  return BigInt(integer) * 10n ** BigInt(scale);
}

export function moveAlphanumericToDecimal(value, options = {}) {
  const scale = options.scale ?? 2;
  return scaledToDecimalString(moveAlphanumericToScaled(value, options), scale);
}
