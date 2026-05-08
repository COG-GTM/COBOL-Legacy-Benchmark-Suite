import { AuditService, AuditEventInput } from '../../services/auditService';
import { InMemoryAuditLogRepository } from '../../repositories/auditRepository';
import {
  AuditType,
  AuditAction,
  AuditStatus,
  SYSTEM_ID,
} from '../../types/audit';

describe('AuditService', () => {
  let repo: InMemoryAuditLogRepository;
  let service: AuditService;

  beforeEach(() => {
    repo = new InMemoryAuditLogRepository();
    service = new AuditService(repo);
  });

  const baseInput: AuditEventInput = {
    method: 'GET',
    path: '/api/portfolios',
    statusCode: 200,
    userId: 'testuser',
    terminal: '127.0.0.1',
  };

  describe('log', () => {
    it('should create an audit entry in the repository', async () => {
      await service.log(baseInput);
      const result = await repo.findMany({});
      expect(result.total).toBe(1);
    });

    it('should set systemId to PORTFOLIO-API', async () => {
      await service.log(baseInput);
      const result = await repo.findMany({});
      expect(result.data[0].systemId).toBe(SYSTEM_ID);
    });

    it('should set timestamp to ISO 8601 format', async () => {
      await service.log(baseInput);
      const result = await repo.findMany({});
      expect(() => new Date(result.data[0].timestamp)).not.toThrow();
      expect(result.data[0].timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T/);
    });

    it('should generate a unique id', async () => {
      await service.log(baseInput);
      await service.log(baseInput);
      const result = await repo.findMany({});
      expect(result.data[0].id).not.toBe(result.data[1].id);
    });

    it('should set message with method, path, and status', async () => {
      await service.log(baseInput);
      const result = await repo.findMany({});
      expect(result.data[0].message).toBe('GET /api/portfolios → 200');
    });

    it('should store portfolioId and accountNo when provided', async () => {
      await service.log({
        ...baseInput,
        portfolioId: 'PORT0001',
        accountNo: '1234567890',
      });
      const result = await repo.findMany({});
      expect(result.data[0].portfolioId).toBe('PORT0001');
      expect(result.data[0].accountNo).toBe('1234567890');
    });

    it('should store before/after images when provided', async () => {
      await service.log({
        ...baseInput,
        method: 'POST',
        beforeImage: '{"name":"old"}',
        afterImage: '{"name":"new"}',
      });
      const result = await repo.findMany({});
      expect(result.data[0].beforeImage).toBe('{"name":"old"}');
      expect(result.data[0].afterImage).toBe('{"name":"new"}');
    });

    it('should not throw when repository insert fails', async () => {
      const failingRepo = {
        insert: jest.fn().mockRejectedValue(new Error('DB down')),
        findMany: jest.fn(),
      };
      const failService = new AuditService(failingRepo);
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      await expect(failService.log(baseInput)).resolves.not.toThrow();
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });

  describe('action resolution', () => {
    it('should map GET to INQUIRE', async () => {
      await service.log({ ...baseInput, method: 'GET' });
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.INQUIRE);
    });

    it('should map POST to CREATE', async () => {
      await service.log({ ...baseInput, method: 'POST', statusCode: 201 });
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.CREATE);
    });

    it('should map PUT to UPDATE', async () => {
      await service.log({ ...baseInput, method: 'PUT' });
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.UPDATE);
    });

    it('should map PATCH to UPDATE', async () => {
      await service.log({ ...baseInput, method: 'PATCH' });
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.UPDATE);
    });

    it('should map DELETE to DELETE', async () => {
      await service.log({ ...baseInput, method: 'DELETE' });
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.DELETE);
    });

    it('should map /auth/login to LOGIN action', async () => {
      await service.log({ ...baseInput, method: 'POST', path: '/auth/login' });
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.LOGIN);
    });

    it('should map /auth/logout to LOGOUT action', async () => {
      await service.log({ ...baseInput, method: 'POST', path: '/auth/logout' });
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.LOGOUT);
    });

    it('should default unknown methods to INQUIRE', async () => {
      await service.log({ ...baseInput, method: 'OPTIONS' });
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.INQUIRE);
    });
  });

  describe('type resolution', () => {
    it('should set type to TRANSACTION for regular API requests', async () => {
      await service.log(baseInput);
      const result = await repo.findMany({});
      expect(result.data[0].type).toBe(AuditType.TRANSACTION);
    });

    it('should set type to USER_ACTION for auth endpoints', async () => {
      await service.log({ ...baseInput, path: '/auth/login' });
      const result = await repo.findMany({});
      expect(result.data[0].type).toBe(AuditType.USER_ACTION);
    });
  });

  describe('status resolution', () => {
    it('should map 2xx to SUCCESS', async () => {
      await service.log({ ...baseInput, statusCode: 200 });
      const result = await repo.findMany({});
      expect(result.data[0].status).toBe(AuditStatus.SUCCESS);
    });

    it('should map 201 to SUCCESS', async () => {
      await service.log({ ...baseInput, statusCode: 201 });
      const result = await repo.findMany({});
      expect(result.data[0].status).toBe(AuditStatus.SUCCESS);
    });

    it('should map 3xx to SUCCESS', async () => {
      await service.log({ ...baseInput, statusCode: 301 });
      const result = await repo.findMany({});
      expect(result.data[0].status).toBe(AuditStatus.SUCCESS);
    });

    it('should map 4xx to WARNING', async () => {
      await service.log({ ...baseInput, statusCode: 404 });
      const result = await repo.findMany({});
      expect(result.data[0].status).toBe(AuditStatus.WARNING);
    });

    it('should map 401 to WARNING', async () => {
      await service.log({ ...baseInput, statusCode: 401 });
      const result = await repo.findMany({});
      expect(result.data[0].status).toBe(AuditStatus.WARNING);
    });

    it('should map 5xx to FAILURE', async () => {
      await service.log({ ...baseInput, statusCode: 500 });
      const result = await repo.findMany({});
      expect(result.data[0].status).toBe(AuditStatus.FAILURE);
    });
  });

  describe('query', () => {
    it('should delegate to repository findMany', async () => {
      await service.log(baseInput);
      await service.log({ ...baseInput, userId: 'other' });
      const result = await service.query({ userId: 'testuser' });
      expect(result.data).toHaveLength(1);
    });
  });
});
