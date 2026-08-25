/**
 * COMP-3 (packed decimal) codec.
 *
 * A COMP-3 field of `digits` total digits occupies Math.floor(digits / 2) + 1
 * bytes.  Each byte holds two 4-bit digits except the last, whose low nibble
 * carries the sign (0xC positive, 0xD negative, 0xF unsigned).  When `digits`
 * is even the high nibble of the first byte is an unused pad nibble.
 *
 * Values are carried as exact decimal strings (never JS floats) so that money
 * comparisons between COBOL and JS are value comparisons, not float noise.
 */

const SIGN_POSITIVE = 0xc;
const SIGN_NEGATIVE = 0xd;
const SIGN_UNSIGNED = 0xf;

export function comp3ByteLength(digits) {
  return Math.floor(digits / 2) + 1;
}

/** Scaled BigInt (value * 10**scale) -> decimal string with `scale` decimals. */
export function scaledToDecimalString(scaled, scale) {
  const negative = scaled < 0n;
  let digits = (negative ? -scaled : scaled).toString().padStart(scale + 1, '0');
  const whole = digits.slice(0, digits.length - scale);
  const frac = scale > 0 ? digits.slice(digits.length - scale) : '';
  const sign = negative ? '-' : '';
  return scale > 0 ? `${sign}${whole}.${frac}` : `${sign}${whole}`;
}

/** Decimal string / number -> scaled BigInt, truncating extra decimals like COBOL MOVE. */
export function decimalStringToScaled(value, scale) {
  const text = typeof value === 'number' ? value.toFixed(scale) : String(value).trim();
  const match = /^([+-]?)(\d*)(?:\.(\d*))?$/.exec(text === '' ? '0' : text);
  if (!match) throw new Error(`not a decimal value: ${JSON.stringify(value)}`);
  const [, sign, whole, frac = ''] = match;
  const fracPadded = frac.padEnd(scale, '0').slice(0, scale);
  const scaled = BigInt(`${whole || '0'}${fracPadded}`);
  return sign === '-' ? -scaled : scaled;
}

/**
 * @param {string|number} value decimal value
 * @param {{digits:number, scale:number, signed?:boolean}} pic
 * @returns {Buffer}
 */
export function encodeComp3(value, { digits, scale, signed = true }) {
  const scaled = decimalStringToScaled(value, scale);
  const negative = scaled < 0n;
  const digitText = (negative ? -scaled : scaled).toString();
  if (digitText.length > digits) {
    throw new Error(`value ${value} exceeds PIC S9(${digits - scale})V9(${scale})`);
  }
  let nibbles = digitText.padStart(digits, '0');
  // Pad to an even nibble count: digits + 1 sign nibble must fill whole bytes.
  if ((digits + 1) % 2 !== 0) nibbles = `0${nibbles}`;
  const signNibble = signed ? (negative ? SIGN_NEGATIVE : SIGN_POSITIVE) : SIGN_UNSIGNED;
  return Buffer.from(`${nibbles}${signNibble.toString(16)}`, 'hex');
}

/**
 * @param {Buffer} buf exactly comp3ByteLength(digits) bytes
 * @param {{digits:number, scale:number}} pic
 * @returns {string} exact decimal string
 */
export function decodeComp3(buf, { digits, scale }) {
  const expected = comp3ByteLength(digits);
  if (buf.length !== expected) {
    throw new Error(`COMP-3 field expects ${expected} bytes, got ${buf.length}`);
  }
  const hex = buf.toString('hex');
  const signNibble = parseInt(hex[hex.length - 1], 16);
  const digitText = hex.slice(0, hex.length - 1);
  if (/[^0-9]/.test(digitText)) {
    throw new Error(`invalid packed-decimal digits: 0x${hex}`);
  }
  const scaled = BigInt(digitText);
  const negative = signNibble === SIGN_NEGATIVE || signNibble === 0xb;
  return scaledToDecimalString(negative ? -scaled : scaled, scale);
}
