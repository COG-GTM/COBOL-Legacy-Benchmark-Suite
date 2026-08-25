'use strict';

const Decimal = require('decimal.js');

function assertField(field) {
  if (!field || field.kind !== 'packed' || !Number.isInteger(field.intDigits)
      || !Number.isInteger(field.fracDigits)) {
    throw new TypeError('A packed-decimal field descriptor is required');
  }
}

function decode(buffer, field) {
  assertField(field);
  if (!Buffer.isBuffer(buffer) || buffer.length !== field.length) {
    throw new RangeError(`Packed field must be exactly ${field.length} bytes`);
  }

  const nibbles = [];
  for (const byte of buffer) {
    nibbles.push((byte >> 4) & 0x0f, byte & 0x0f);
  }
  const sign = nibbles.pop();
  if (sign !== 0x0c && sign !== 0x0d) {
    throw new RangeError(`Invalid COMP-3 sign nibble: ${sign.toString(16)}`);
  }

  const digitCount = field.intDigits + field.fracDigits;
  const leading = nibbles.length - digitCount;
  if (leading < 0 || nibbles.slice(0, leading).some((nibble) => nibble !== 0)
      || nibbles.slice(leading).some((nibble) => nibble > 9)) {
    throw new RangeError('Invalid COMP-3 digit nibbles');
  }
  const digits = nibbles.slice(leading).join('');
  const unscaled = new Decimal(digits || '0');
  const value = unscaled.div(new Decimal(10).pow(field.fracDigits));
  return sign === 0x0d ? value.negated() : value;
}

function encode(decimal, field) {
  assertField(field);
  const value = Decimal.isDecimal(decimal) ? decimal : new Decimal(decimal);
  if (!value.isFinite() || value.decimalPlaces() > field.fracDigits) {
    throw new RangeError('COMP-3 value has more fractional digits than the field');
  }

  const absolute = value.abs();
  const fixed = absolute.toFixed(field.fracDigits);
  const digits = fixed.replace('.', '').padStart(field.intDigits + field.fracDigits, '0');
  if (digits.length > field.intDigits + field.fracDigits
      || absolute.gte(new Decimal(10).pow(field.intDigits))) {
    throw new RangeError('COMP-3 value overflows integer digits');
  }

  const allNibbles = [];
  const digitCount = field.intDigits + field.fracDigits;
  if (digitCount % 2 === 0) allNibbles.push(0);
  for (const digit of digits) allNibbles.push(Number(digit));
  allNibbles.push(value.isNegative() ? 0x0d : 0x0c);
  const output = Buffer.alloc(field.length);
  for (let index = 0; index < output.length; index += 1) {
    output[index] = (allNibbles[index * 2] << 4) | allNibbles[index * 2 + 1];
  }
  return output;
}

module.exports = { decode, encode };
