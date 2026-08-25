'use strict';

const Decimal = require('decimal.js');
const { createSystem, toCanonicalJson } = require('../src');

test('handler routes canonical CRUD actions and audit trail', async () => {
  const system = createSystem({ mode: 'legacy', runDate: '20260825' });
  const portfolio = {
    portId: 'PORT0001', accountNo: '0000000001', clientName: 'Acme', status: 'A',
    totalValue: new Decimal('10.00'), cashBalance: new Decimal('2.00'),
  };
  expect((await system.handler({ action: 'create', record: portfolio })).http).toBe(201);
  expect((await system.handler({ action: 'read', key: {
    portId: 'PORT0001', accountNo: '0000000001',
  } })).http).toBe(200);
  expect((await system.handler({ action: 'list' })).count).toBe(1);
  expect((await system.handler({
    action: 'update', key: { portId: 'PORT0001', accountNo: '0000000001' },
    updateAction: 'V', newValue: '99999.99',
  })).record.totalValue.toFixed(2)).toBe('99999.99');
  expect((await system.handler({
    action: 'delete', key: { portId: 'PORT0001', accountNo: '0000000001' }, reasonCode: '03',
  })).http).toBe(200);
  expect((await system.handler({ action: 'auditTrail' })).records).toHaveLength(1);
  expect(toCanonicalJson(portfolio).totalValue).toBe('10.00');
});

test('unknown action follows Invalid command path', async () => {
  const { handler } = createSystem({ runDate: '20260825' });
  expect(await handler({ action: 'bogus' })).toMatchObject({
    result: 'invalidCommand', http: 400, message: 'Invalid command',
  });
});

test('legacy transaction validates but does not mutate', async () => {
  const system = createSystem({
    mode: 'legacy',
    runDate: '20260825',
    seed: [{
      portId: 'PORT0001', accountNo: '0000000001', status: 'A',
      totalUnits: new Decimal('5.0000'), totalCost: new Decimal('10.00'),
    }],
  });
  await system.handler({
    action: 'transaction',
    transaction: { portfolioId: 'PORT0001', type: 'BU', quantity: new Decimal('2'), price: new Decimal('1'), amount: new Decimal('2') },
  });
  const result = await system.handler({ action: 'read', key: { portId: 'PORT0001', accountNo: '0000000001' } });
  expect(result.record.totalUnits.toFixed(4)).toBe('5.0000');
});

test('modernized transaction applies Decimal position math and audits failures', async () => {
  const system = createSystem({
    mode: 'modernized',
    runDate: '20260825',
    seed: [{
      portId: 'PORT0001', accountNo: '0000000001', status: 'A',
      totalUnits: new Decimal('100.0000'), totalCost: new Decimal('1000.00'),
    }],
  });
  expect((await system.handler({
    action: 'transaction',
    transaction: { portfolioId: 'PORT0001', type: 'BU', quantity: new Decimal('2'), price: new Decimal('1'), amount: new Decimal('2') },
  })).result).toBe('ok');
  expect((await system.handler({
    action: 'transaction',
    transaction: { portfolioId: 'PORT0001', type: 'SL', quantity: new Decimal('999'), price: new Decimal('1'), amount: new Decimal('2') },
  })).message).toBe('Insufficient units for sale');
  const result = await system.handler({ action: 'read', key: { portId: 'PORT0001', accountNo: '0000000001' } });
  expect(result.record.totalUnits.toFixed(4)).toBe('102.0000');
  expect((await system.handler({ action: 'auditTrail' })).records).toHaveLength(2);
});
