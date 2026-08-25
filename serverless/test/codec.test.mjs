import assert from 'node:assert/strict';
import test from 'node:test';

import {
  decodeComp3,
  encodeComp3,
} from '../src/codec/comp3.js';
import { LAYOUTS, recordLength } from '../src/codec/layouts.js';
import { decodeRecord, encodeRecord } from '../src/codec/record.js';

test('COMP-3 round trips boundary and fractional values exactly', () => {
  const values = [
    '-9999999999999.99',
    '9999999999999.99',
    '0.00',
    '0.01',
  ];

  for (const value of values) {
    const encoded = encodeComp3(value, { digits: 15, scale: 2 });
    assert.equal(decodeComp3(encoded, { digits: 15, scale: 2 }), value);
  }
});

test('all authoritative record layouts have their expected lengths', () => {
  assert.equal(recordLength(LAYOUTS.PORT_RECORD), 148);
  assert.equal(recordLength(LAYOUTS.TRANSACTION_RECORD), 152);
  assert.equal(recordLength(LAYOUTS.UPDATE_RECORD), 69);
  assert.equal(recordLength(LAYOUTS.DELETE_RECORD), 80);
  assert.equal(recordLength(LAYOUTS.DELETE_AUDIT_RECORD), 80);
  assert.equal(recordLength(LAYOUTS.VALIDATION_REQUEST_RECORD), 61);
});

test('a complete PORT_RECORD survives encode/decode', () => {
  const record = {
    portId: 'PORT0001',
    accountNo: '1000000001',
    clientName: 'ALICE GROWTH FUND',
    clientType: 'I',
    createDate: '20240320',
    lastMaint: '20240321',
    status: 'A',
    totalValue: '12345678.99',
    cashBalance: '1000.00',
    lastUser: 'TESTUSR1',
    lastTrans: '20240321',
    filler: '',
  };

  assert.deepEqual(decodeRecord(LAYOUTS.PORT_RECORD, encodeRecord(LAYOUTS.PORT_RECORD, record)), record);
});

test('COMP-3 rejects values wider than the PIC', () => {
  assert.throws(
    () => encodeComp3('10000000000000.00', { digits: 15, scale: 2 }),
    /exceeds PIC/,
  );
});
