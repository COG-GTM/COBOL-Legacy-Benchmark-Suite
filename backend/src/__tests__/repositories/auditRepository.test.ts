import { InMemoryAuditLogRepository } from '../../repositories/auditRepository';
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

describe('InMemoryAuditLogRepository', () => {
  let repo: InMemoryAuditLogRepository;

  beforeEach(() => {
    repo = new InMemoryAuditLogRepository();
  });

  describe('insert', () => {
    it('should insert an entry', async () => {
      const entry = createEntry();
      await repo.insert(entry);
      expect(repo.count()).toBe(1);
    });

    it('should insert multiple entries', async () => {
      await repo.insert(createEntry());
      await repo.insert(createEntry());
      await repo.insert(createEntry());
      expect(repo.count()).toBe(3);
    });
  });

  describe('findMany', () => {
    it('should return empty result when no entries exist', async () => {
      const result = await repo.findMany({});
      expect(result.data).toHaveLength(0);
      expect(result.total).toBe(0);
    });

    it('should return all entries with no filters', async () => {
      await repo.insert(createEntry());
      await repo.insert(createEntry());
      const result = await repo.findMany({});
      expect(result.data).toHaveLength(2);
      expect(result.total).toBe(2);
    });

    it('should filter by userId', async () => {
      await repo.insert(createEntry({ userId: 'alice' }));
      await repo.insert(createEntry({ userId: 'bob' }));
      await repo.insert(createEntry({ userId: 'alice' }));
      const result = await repo.findMany({ userId: 'alice' });
      expect(result.data).toHaveLength(2);
      expect(result.data.every((e) => e.userId === 'alice')).toBe(true);
    });

    it('should filter by action', async () => {
      await repo.insert(createEntry({ action: AuditAction.CREATE }));
      await repo.insert(createEntry({ action: AuditAction.INQUIRE }));
      await repo.insert(createEntry({ action: AuditAction.CREATE }));
      const result = await repo.findMany({ action: AuditAction.CREATE });
      expect(result.data).toHaveLength(2);
    });

    it('should filter by type', async () => {
      await repo.insert(createEntry({ type: AuditType.TRANSACTION }));
      await repo.insert(createEntry({ type: AuditType.USER_ACTION }));
      const result = await repo.findMany({ type: AuditType.USER_ACTION });
      expect(result.data).toHaveLength(1);
    });

    it('should filter by status', async () => {
      await repo.insert(createEntry({ status: AuditStatus.SUCCESS }));
      await repo.insert(createEntry({ status: AuditStatus.FAILURE }));
      const result = await repo.findMany({ status: AuditStatus.FAILURE });
      expect(result.data).toHaveLength(1);
    });

    it('should filter by portfolioId', async () => {
      await repo.insert(createEntry({ portfolioId: 'PORT0001' }));
      await repo.insert(createEntry({ portfolioId: 'PORT0002' }));
      const result = await repo.findMany({ portfolioId: 'PORT0001' });
      expect(result.data).toHaveLength(1);
      expect(result.data[0].portfolioId).toBe('PORT0001');
    });

    it('should filter by date range', async () => {
      await repo.insert(createEntry({ timestamp: '2025-01-01T00:00:00Z' }));
      await repo.insert(createEntry({ timestamp: '2025-06-15T00:00:00Z' }));
      await repo.insert(createEntry({ timestamp: '2025-12-31T00:00:00Z' }));
      const result = await repo.findMany({
        startDate: '2025-03-01T00:00:00Z',
        endDate: '2025-09-01T00:00:00Z',
      });
      expect(result.data).toHaveLength(1);
      expect(result.data[0].timestamp).toBe('2025-06-15T00:00:00Z');
    });

    it('should apply pagination with limit and offset', async () => {
      for (let i = 0; i < 10; i++) {
        await repo.insert(
          createEntry({ timestamp: `2025-01-${String(i + 1).padStart(2, '0')}T00:00:00Z` }),
        );
      }
      const result = await repo.findMany({ limit: 3, offset: 2 });
      expect(result.data).toHaveLength(3);
      expect(result.total).toBe(10);
      expect(result.limit).toBe(3);
      expect(result.offset).toBe(2);
    });

    it('should cap limit at 100', async () => {
      const result = await repo.findMany({ limit: 200 });
      expect(result.limit).toBe(100);
    });

    it('should default limit to 50', async () => {
      const result = await repo.findMany({});
      expect(result.limit).toBe(50);
    });

    it('should sort by timestamp descending', async () => {
      await repo.insert(createEntry({ timestamp: '2025-01-01T00:00:00Z' }));
      await repo.insert(createEntry({ timestamp: '2025-12-31T00:00:00Z' }));
      await repo.insert(createEntry({ timestamp: '2025-06-15T00:00:00Z' }));
      const result = await repo.findMany({});
      expect(result.data[0].timestamp).toBe('2025-12-31T00:00:00Z');
      expect(result.data[1].timestamp).toBe('2025-06-15T00:00:00Z');
      expect(result.data[2].timestamp).toBe('2025-01-01T00:00:00Z');
    });

    it('should combine multiple filters', async () => {
      await repo.insert(
        createEntry({ userId: 'alice', action: AuditAction.CREATE, status: AuditStatus.SUCCESS }),
      );
      await repo.insert(
        createEntry({ userId: 'alice', action: AuditAction.CREATE, status: AuditStatus.FAILURE }),
      );
      await repo.insert(
        createEntry({ userId: 'bob', action: AuditAction.CREATE, status: AuditStatus.SUCCESS }),
      );
      const result = await repo.findMany({
        userId: 'alice',
        action: AuditAction.CREATE,
        status: AuditStatus.SUCCESS,
      });
      expect(result.data).toHaveLength(1);
    });
  });

  describe('clear', () => {
    it('should remove all entries', async () => {
      await repo.insert(createEntry());
      await repo.insert(createEntry());
      repo.clear();
      expect(repo.count()).toBe(0);
    });
  });
});
