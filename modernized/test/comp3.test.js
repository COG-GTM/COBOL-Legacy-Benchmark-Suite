'use strict';

const fs = require('fs');
const path = require('path');
const Decimal = require('decimal.js');
const { decode, encode } = require('../src/codec/comp3');

const table = require('../../golden/vectors/comp3-vectors.json').vectors;
const raw = fs.readFileSync(path.join(__dirname, '../../golden/vectors/comp3-vectors.bin'));

test.each(table)('COMP-3 vector %i', (vector) => {
  const field = {
    kind: 'packed',
    length: 8,
    intDigits: vector.intDigits,
    fracDigits: vector.fracDigits,
  };
  const bytes = raw.subarray((vector.index - 1) * 8, vector.index * 8);
  expect(bytes.toString('hex').toUpperCase()).toBe(vector.hex);
  expect(encode(new Decimal(vector.value), field)).toEqual(bytes);
  expect(decode(bytes, field).toFixed(vector.fracDigits)).toBe(vector.value);
});

test('COMP-3 rejects integer overflow', () => {
  expect(() => encode(new Decimal('10000000000000'), {
    kind: 'packed', length: 8, intDigits: 13, fracDigits: 2,
  })).toThrow(/overflows/);
});
