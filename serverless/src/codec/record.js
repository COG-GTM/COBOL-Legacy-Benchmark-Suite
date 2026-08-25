/**
 * Fixed-width record <-> canonical JSON codec.
 *
 * Canonical form rules (this is the "compare decimal values, not raw bytes"
 * layer that mirrors what TSTVAL00 does inside COBOL):
 *   - 'X'     fields decode to strings with trailing spaces trimmed
 *   - '9'     fields decode to their digit string as stored (zero padding kept,
 *             because COBOL 9(8) dates are positional)
 *   - 'comp3' fields decode to exact decimal strings, e.g. "-1234.56"
 * Encoding is the exact inverse, so decode(encode(x)) === x.
 */

import { decodeComp3, encodeComp3 } from './comp3.js';
import { recordLength } from './layouts.js';

const EBCDIC_SAFE_ENCODING = 'latin1';

function encodeField(field, value) {
  switch (field.kind) {
    case 'X': {
      const text = value === undefined || value === null ? '' : String(value);
      if (text.length > field.size) {
        throw new Error(`${field.name}: "${text}" exceeds PIC X(${field.size})`);
      }
      return Buffer.from(text.padEnd(field.size, ' '), EBCDIC_SAFE_ENCODING);
    }
    case '9': {
      const text = value === undefined || value === null ? '' : String(value).trim();
      const digits = text === '' ? '0' : text;
      if (!/^\d+$/.test(digits)) {
        throw new Error(`${field.name}: "${text}" is not numeric for PIC 9(${field.size})`);
      }
      if (digits.length > field.size) {
        throw new Error(`${field.name}: "${text}" exceeds PIC 9(${field.size})`);
      }
      return Buffer.from(digits.padStart(field.size, '0'), EBCDIC_SAFE_ENCODING);
    }
    case 'comp3':
      return encodeComp3(value === undefined || value === null ? '0' : value, field);
    default:
      throw new Error(`unsupported field kind ${field.kind}`);
  }
}

function decodeField(field, buf) {
  switch (field.kind) {
    case 'X':
      return buf.toString(EBCDIC_SAFE_ENCODING).replace(/ +$/, '');
    case '9':
      return buf.toString(EBCDIC_SAFE_ENCODING);
    case 'comp3':
      return decodeComp3(buf, field);
    default:
      throw new Error(`unsupported field kind ${field.kind}`);
  }
}

/** @returns {Buffer} one fixed-format record */
export function encodeRecord(layout, values) {
  const unknown = Object.keys(values).filter(
    (key) => !layout.fields.some((field) => field.name === key),
  );
  if (unknown.length > 0) {
    throw new Error(`${layout.name}: unknown field(s) ${unknown.join(', ')}`);
  }
  return Buffer.concat(layout.fields.map((field) => encodeField(field, values[field.name])));
}

/** @returns {object} canonical JSON form of one record */
export function decodeRecord(layout, buf) {
  const expected = recordLength(layout);
  if (buf.length !== expected) {
    throw new Error(`${layout.name}: expected ${expected} bytes, got ${buf.length}`);
  }
  const out = {};
  let offset = 0;
  for (const field of layout.fields) {
    out[field.name] = decodeField(field, buf.subarray(offset, offset + field.size));
    offset += field.size;
  }
  return out;
}

export function encodeFile(layout, records) {
  return Buffer.concat(records.map((record) => encodeRecord(layout, record)));
}

export function decodeFile(layout, buf) {
  const size = recordLength(layout);
  if (buf.length % size !== 0) {
    throw new Error(
      `${layout.name}: file length ${buf.length} is not a multiple of record length ${size}`,
    );
  }
  const records = [];
  for (let offset = 0; offset < buf.length; offset += size) {
    records.push(decodeRecord(layout, buf.subarray(offset, offset + size)));
  }
  return records;
}
