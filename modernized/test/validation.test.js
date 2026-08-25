'use strict';

const fs = require('fs');
const path = require('path');
const { validateLegacy, validateModernized, divergences } = require('../src/validation');

const lines = fs.readFileSync(path.join(__dirname, '../../golden/expected/portvald.txt'), 'utf8')
  .trimEnd().split('\n');

test('legacy PORTVALD reproduces every executed vector', () => {
  for (const line of lines) {
    const [, caseId, type, input, rc, message] = line.split('|');
    expect(caseId.trim()).toMatch(/^VAL-\d\d$/);
    const actual = validateLegacy(type.trim(), input.trim());
    expect(actual.code).toBe(Number(rc.trim()));
    expect(actual.message).toBe(message.trim());
  }
});

test('modernized validation follows documented intent', () => {
  expect(validateModernized('I', 'PORT0001').code).toBe(0);
  expect(validateModernized('A', '1234567890').code).toBe(0);
  expect(validateModernized('T', 'BND').code).toBe(0);
  expect(validateModernized('M', 'not a number').code).toBe(4);
  expect(divergences.map((entry) => entry.type)).toEqual(['I', 'A', 'M']);
});
