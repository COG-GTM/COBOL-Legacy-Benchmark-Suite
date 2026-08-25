'use strict';

const Decimal = require('decimal.js');
const { decodeRecord, encodeRecord } = require('../src/codec/fixed');
const { PORT_RECORD } = require('../src/schema/records');

test('fixed record codec round-trips display, dates, and packed fields', () => {
  const record = {
    'PORT-ID': 'PORT0001',
    'PORT-ACCOUNT-NO': '0000000001',
    'PORT-CLIENT-NAME': 'Acme',
    'PORT-CLIENT-TYPE': 'I',
    'PORT-CREATE-DATE': '20250101',
    'PORT-LAST-MAINT': null,
    'PORT-STATUS': 'A',
    'PORT-TOTAL-VALUE': new Decimal('12.34'),
    'PORT-CASH-BALANCE': new Decimal('-0.05'),
    'PORT-LAST-USER': 'USER',
    'PORT-LAST-TRANS': null,
    'PORT-FILLER': '',
  };
  expect(decodeRecord(encodeRecord(record, PORT_RECORD), PORT_RECORD)).toEqual(record);
});

test('fixed codec rejects over-length display fields', () => {
  expect(() => encodeRecord({ 'PORT-ID': '123456789' }, PORT_RECORD)).toThrow();
});
