'use strict';

const Decimal = require('decimal.js');
const comp3 = require('./comp3');

function decodeRecord(buffer, layout) {
  if (!Buffer.isBuffer(buffer) || buffer.length !== layout.length) {
    throw new RangeError(`${layout.name} must be exactly ${layout.length} bytes`);
  }
  const record = {};
  for (const field of layout.fields) {
    const slice = buffer.subarray(field.offset, field.offset + field.length);
    if (field.kind === 'alnum') {
      record[field.name] = slice.toString('ascii').replace(/ +$/, '');
    } else if (field.kind === 'digits') {
      const value = slice.toString('ascii');
      if (!/^[0-9]+$/.test(value)) throw new RangeError(`Invalid digits in ${field.name}`);
      record[field.name] = /^0+$/.test(value) ? null : value;
    } else {
      record[field.name] = comp3.decode(slice, field);
    }
  }
  return record;
}

function stringValue(value) {
  return value === null || value === undefined ? '' : String(value);
}

function encodeRecord(record, layout) {
  const output = Buffer.alloc(layout.length, 0x20);
  for (const field of layout.fields) {
    const value = record[field.name];
    if (field.kind === 'alnum') {
      const text = stringValue(value);
      if (Buffer.byteLength(text, 'ascii') > field.length) {
        throw new RangeError(`${field.name} exceeds ${field.length} bytes`);
      }
      output.write(text, field.offset, field.length, 'ascii');
    } else if (field.kind === 'digits') {
      const text = value === null || value === undefined ? '0'.repeat(field.length) : String(value);
      if (!/^[0-9]+$/.test(text) || text.length > field.length) {
        throw new RangeError(`${field.name} must be at most ${field.length} digits`);
      }
      output.write(text.padStart(field.length, '0'), field.offset, field.length, 'ascii');
    } else {
      const packed = comp3.encode(
        Decimal.isDecimal(value) ? value : new Decimal(value || 0),
        field
      );
      packed.copy(output, field.offset);
    }
  }
  return output;
}

module.exports = { decodeRecord, encodeRecord };
