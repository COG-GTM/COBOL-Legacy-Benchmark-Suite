import assert from 'node:assert/strict';
import test from 'node:test';

import { IndexedFile, STATUS } from '../src/store/indexed-file.js';

const record = (portId, accountNo) => ({
  portId,
  accountNo,
  clientName: '',
  clientType: 'I',
  createDate: '20240101',
  lastMaint: '20240101',
  status: 'A',
  totalValue: '0.00',
  cashBalance: '0.00',
  lastUser: '',
  lastTrans: '00000000',
  filler: '',
});

test('writing a duplicate returns VSAM status 22', () => {
  const file = new IndexedFile([record('PORT0001', '1000000001')]);
  assert.equal(file.write(record('PORT0001', '1000000001')).status, STATUS.DUPLICATE_KEY);
});

test('missing read, rewrite, and delete return VSAM status 23', () => {
  const file = new IndexedFile();
  const key = 'PORT9999' + '9999999999';
  assert.equal(file.read(key).status, STATUS.NOT_FOUND);
  assert.equal(file.rewrite(record('PORT9999', '9999999999')).status, STATUS.NOT_FOUND);
  assert.equal(file.delete(key).status, STATUS.NOT_FOUND);
});

test('browse returns key order rather than insertion order', () => {
  const file = new IndexedFile([
    record('PORT0003', '1000000003'),
    record('PORT0001', '1000000001'),
    record('PORT0002', '2000000002'),
    record('PORT0002', '1000000002'),
  ]);

  assert.deepEqual(
    file.browse().map(({ portId, accountNo }) => `${portId}${accountNo}`),
    [
      'PORT00011000000001',
      'PORT00021000000002',
      'PORT00022000000002',
      'PORT00031000000003',
    ],
  );
});
