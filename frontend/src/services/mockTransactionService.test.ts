import { describe, expect, it } from 'vitest';
import { MockTransactionService } from './mockTransactionService';
import {
  InsufficientUnitsError,
  UnknownPortfolioError,
} from './transactionService';
import type { TransactionInput } from '../types/transaction';

const BUY: TransactionInput = {
  portfolioId: 'PORT0001',
  investmentId: 'FND0000001',
  type: 'BU',
  quantity: '10.0000',
  price: '400.5000',
  currency: 'USD',
};

describe('MockTransactionService', () => {
  it('lists transactions newest first', async () => {
    const service = new MockTransactionService();
    const results = await service.list();
    expect(results.length).toBeGreaterThan(0);
    const keys = results.map((t) => `${t.date}${t.time}`);
    expect([...keys].sort().reverse()).toEqual(keys);
  });

  it('filters by portfolio, status and type', async () => {
    const service = new MockTransactionService();
    expect(
      (await service.list({ portfolioId: 'port0001' })).every(
        (t) => t.portfolioId === 'PORT0001',
      ),
    ).toBe(true);
    expect(
      (await service.list({ status: 'F' })).every((t) => t.status === 'F'),
    ).toBe(true);
    expect(
      (await service.list({ type: 'TR' })).every((t) => t.type === 'TR'),
    ).toBe(true);
  });

  it('stamps the key, amount and pending status on submit', async () => {
    const service = new MockTransactionService();
    const created = await service.submit(BUY);

    expect(created.status).toBe('P');
    expect(created.amount).toBe('4005.00');
    expect(created.date).toMatch(/^\d{8}$/);
    expect(created.time).toMatch(/^\d{6}$/);
    expect(created.sequenceNo).toMatch(/^\d{6}$/);

    const listed = await service.list({ portfolioId: 'PORT0001' });
    expect(listed.some((t) => t.sequenceNo === created.sequenceNo)).toBe(true);
  });

  it('rejects an unknown portfolio (PORTTRAN 2110-CHECK-PORTFOLIO)', async () => {
    const service = new MockTransactionService();
    await expect(
      service.submit({ ...BUY, portfolioId: 'PORT9999' }),
    ).rejects.toBeInstanceOf(UnknownPortfolioError);
  });

  it('rejects a sell larger than the units held', async () => {
    const service = new MockTransactionService();
    await expect(
      service.submit({ ...BUY, type: 'SL', quantity: '99999.0000' }),
    ).rejects.toBeInstanceOf(InsufficientUnitsError);
  });

  it('reduces available units by sells that are still pending', async () => {
    const service = new MockTransactionService();
    const before = await service.availableUnits('PORT0001', 'FND0000001');
    expect(before).toBe('1250.5000');

    await service.submit({ ...BUY, type: 'SL' });

    expect(await service.availableUnits('PORT0001', 'FND0000001')).toBe(
      '1240.5000',
    );
  });

  it('reports no holding for an investment the portfolio does not own', async () => {
    const service = new MockTransactionService();
    expect(await service.availableUnits('PORT0001', 'FND0009999')).toBeNull();
  });
});
