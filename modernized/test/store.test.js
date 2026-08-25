'use strict';

const { DocumentStore } = require('../src/store');

const record = (id, account) => ({ portId: id, accountNo: account });

test('store implements KSDS statuses and key order', () => {
  const store = new DocumentStore();
  expect(store.read({ portId: 'PORT0001', accountNo: '0000000001' }).status).toBe('23');
  expect(store.write(record('PORT0002', '0000000001')).status).toBe('00');
  expect(store.write(record('PORT0001', '0000000001')).status).toBe('00');
  expect(store.write(record('PORT0001', '0000000001')).status).toBe('22');
  expect(store.readNext(0).record.portId).toBe('PORT0001');
  expect(store.readNext(1).record.portId).toBe('PORT0002');
  expect(store.readNext(2).status).toBe('10');
});

test('audit store appends records', () => {
  const store = new DocumentStore();
  expect(store.audit.append({ action: 'DELETE' }).status).toBe('00');
  expect(store.audit.records).toHaveLength(1);
});
