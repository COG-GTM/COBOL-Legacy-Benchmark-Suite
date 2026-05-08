import express from 'express';
import request from 'supertest';
import { InMemoryAuditLogRepository } from '../../repositories/auditRepository';
import { AuditService } from '../../services/auditService';
import { createAuditLogsRouter } from '../../routes/auditLogs';
import {
  AuditLogEntry,
  AuditType,
  AuditAction,
  AuditStatus,
} from '../../types/audit';

function createEntry(overrides: Partial<AuditLogEntry> = {}): AuditLogEntry {
  return {
    id: `test-${Date.now()}-${Math.random()}`,
    timestamp: new Date().toISOString(),
    systemId: 'PORTFOLIO-API',
    userId: 'testuser',
    resource: '/api/portfolios',
    terminal: '127.0.0.1',
    type: AuditType.TRANSACTION,
    action: AuditAction.INQUIRE,
    status: AuditStatus.SUCCESS,
    message: 'GET /api/portfolios → 200',
    ...overrides,
  };
}

function createApp(repo: InMemoryAuditLogRepository) {
  const app = express();
  app.use(express.json());
  const auditService = new AuditService(repo);
  app.use('/api/audit-logs', createAuditLogsRouter({ auditService }));
  return app;
}

describe('GET /api/audit-logs', () => {
  let repo: InMemoryAuditLogRepository;

  beforeEach(() => {
    repo = new InMemoryAuditLogRepository();
  });

  it('should return empty result when no logs exist', async () => {
    const app = createApp(repo);
    const res = await request(app).get('/api/audit-logs').expect(200);
    expect(res.body.data).toHaveLength(0);
    expect(res.body.total).toBe(0);
  });

  it('should return all logs', async () => {
    await repo.insert(createEntry());
    await repo.insert(createEntry());
    const app = createApp(repo);
    const res = await request(app).get('/api/audit-logs').expect(200);
    expect(res.body.data).toHaveLength(2);
    expect(res.body.total).toBe(2);
  });

  it('should filter by userId', async () => {
    await repo.insert(createEntry({ userId: 'alice' }));
    await repo.insert(createEntry({ userId: 'bob' }));
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?userId=alice')
      .expect(200);
    expect(res.body.data).toHaveLength(1);
    expect(res.body.data[0].userId).toBe('alice');
  });

  it('should filter by action', async () => {
    await repo.insert(createEntry({ action: AuditAction.CREATE }));
    await repo.insert(createEntry({ action: AuditAction.INQUIRE }));
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?action=CREATE')
      .expect(200);
    expect(res.body.data).toHaveLength(1);
    expect(res.body.data[0].action).toBe('CREATE');
  });

  it('should filter by type', async () => {
    await repo.insert(createEntry({ type: AuditType.TRANSACTION }));
    await repo.insert(createEntry({ type: AuditType.USER_ACTION }));
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?type=USER')
      .expect(200);
    expect(res.body.data).toHaveLength(1);
  });

  it('should filter by status', async () => {
    await repo.insert(createEntry({ status: AuditStatus.SUCCESS }));
    await repo.insert(createEntry({ status: AuditStatus.FAILURE }));
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?status=FAIL')
      .expect(200);
    expect(res.body.data).toHaveLength(1);
  });

  it('should filter by portfolioId', async () => {
    await repo.insert(createEntry({ portfolioId: 'PORT0001' }));
    await repo.insert(createEntry({ portfolioId: 'PORT0002' }));
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?portfolioId=PORT0001')
      .expect(200);
    expect(res.body.data).toHaveLength(1);
  });

  it('should filter by date range', async () => {
    await repo.insert(createEntry({ timestamp: '2025-01-01T00:00:00Z' }));
    await repo.insert(createEntry({ timestamp: '2025-06-15T00:00:00Z' }));
    await repo.insert(createEntry({ timestamp: '2025-12-31T00:00:00Z' }));
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?startDate=2025-03-01T00:00:00Z&endDate=2025-09-01T00:00:00Z')
      .expect(200);
    expect(res.body.data).toHaveLength(1);
  });

  it('should apply pagination with limit and offset', async () => {
    for (let i = 0; i < 10; i++) {
      await repo.insert(
        createEntry({
          timestamp: `2025-01-${String(i + 1).padStart(2, '0')}T00:00:00Z`,
        }),
      );
    }
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?limit=3&offset=2')
      .expect(200);
    expect(res.body.data).toHaveLength(3);
    expect(res.body.total).toBe(10);
    expect(res.body.limit).toBe(3);
    expect(res.body.offset).toBe(2);
  });

  it('should ignore invalid action values', async () => {
    await repo.insert(createEntry());
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?action=INVALID')
      .expect(200);
    expect(res.body.data).toHaveLength(1);
  });

  it('should ignore invalid limit values', async () => {
    const app = createApp(repo);
    const res = await request(app)
      .get('/api/audit-logs?limit=-5')
      .expect(200);
    expect(res.body.limit).toBe(50);
  });

  it('should return proper pagination metadata', async () => {
    const app = createApp(repo);
    const res = await request(app).get('/api/audit-logs').expect(200);
    expect(res.body).toHaveProperty('data');
    expect(res.body).toHaveProperty('total');
    expect(res.body).toHaveProperty('limit');
    expect(res.body).toHaveProperty('offset');
  });
});
