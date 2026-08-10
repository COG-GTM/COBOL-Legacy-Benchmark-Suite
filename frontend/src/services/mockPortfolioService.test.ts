import { beforeEach, describe, expect, it } from 'vitest';
import type { Portfolio, PortfolioInput } from '../types/portfolio';
import { MockPortfolioService } from './mockPortfolioService';
import {
  DuplicatePortfolioError,
  PortfolioNotFoundError,
} from './portfolioService';

const seed: Portfolio[] = [
  {
    portId: 'PORT0001',
    accountNo: 'ACCT100001',
    clientName: 'Margaret Chen',
    clientType: 'I',
    createDate: '20240115',
    lastMaintDate: '20240320',
    status: 'A',
    totalValue: '1000.00',
    cashBalance: '100.00',
    lastUser: 'JSMITH',
    lastTransId: '00000005',
  },
  {
    portId: 'PORT0002',
    accountNo: 'ACCT100002',
    clientName: 'Atlas Holdings LLC',
    clientType: 'C',
    createDate: '20230903',
    lastMaintDate: '20240218',
    status: 'C',
    totalValue: '2000.00',
    cashBalance: '0.00',
    lastUser: 'MGARCIA',
    lastTransId: '00000010',
  },
];

const newInput: PortfolioInput = {
  portId: 'PORT0003',
  accountNo: 'ACCT100003',
  clientName: 'New Client',
  clientType: 'T',
  status: 'A',
  totalValue: '500.5',
  cashBalance: '10',
};

describe('MockPortfolioService', () => {
  let service: MockPortfolioService;

  beforeEach(() => {
    service = new MockPortfolioService(seed, 'WEBUSER');
  });

  it('lists all portfolios sorted by id', async () => {
    const result = await service.list();
    expect(result.map((p) => p.portId)).toEqual(['PORT0001', 'PORT0002']);
  });

  it('filters by account number, client name, and status', async () => {
    expect((await service.list({ accountNo: '100002' }))).toHaveLength(1);
    expect((await service.list({ clientName: 'chen' }))[0].portId).toBe(
      'PORT0001',
    );
    expect((await service.list({ status: 'C' }))[0].portId).toBe('PORT0002');
    expect(await service.list({ clientName: 'nobody' })).toHaveLength(0);
  });

  it('creates a portfolio, normalizing money and stamping audit fields', async () => {
    const created = await service.create(newInput);
    expect(created.totalValue).toBe('500.50');
    expect(created.cashBalance).toBe('10.00');
    expect(created.lastUser).toBe('WEBUSER');
    expect(created.createDate).toMatch(/^\d{8}$/);
    expect(created.lastTransId).toBe('00000011');
    expect(await service.get('PORT0003')).toBeDefined();
  });

  it('rejects duplicate ids on create', async () => {
    await expect(
      service.create({ ...newInput, portId: 'PORT0001' }),
    ).rejects.toBeInstanceOf(DuplicatePortfolioError);
  });

  it('updates a portfolio but preserves the key and create date', async () => {
    const updated = await service.update('PORT0001', {
      ...newInput,
      portId: 'IGNORED',
    });
    expect(updated.portId).toBe('PORT0001');
    expect(updated.createDate).toBe('20240115');
    expect(updated.clientName).toBe('New Client');
  });

  it('throws when updating or deleting a missing portfolio', async () => {
    await expect(service.update('NOPE', newInput)).rejects.toBeInstanceOf(
      PortfolioNotFoundError,
    );
    await expect(service.remove('NOPE')).rejects.toBeInstanceOf(
      PortfolioNotFoundError,
    );
  });

  it('deletes a portfolio', async () => {
    await service.remove('PORT0001');
    expect(await service.get('PORT0001')).toBeUndefined();
    expect(await service.list()).toHaveLength(1);
  });
});
