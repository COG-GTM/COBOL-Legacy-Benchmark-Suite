import express, { Request, Response } from 'express';
import request from 'supertest';
import { InMemoryAuditLogRepository } from '../../repositories/auditRepository';
import { AuditService } from '../../services/auditService';
import { createAuditMiddleware } from '../../middleware/audit';
import { AuditAction, AuditStatus } from '../../types/audit';

function createApp(repo: InMemoryAuditLogRepository, excludePatterns: RegExp[] = []) {
  const app = express();
  app.use(express.json());

  const auditService = new AuditService(repo);

  app.use((req: Request, _res: Response, next) => {
    (req as Request & { user?: { userId: string } }).user = {
      userId: 'testuser',
    };
    next();
  });

  app.use(createAuditMiddleware({ auditService, excludePatterns }));

  app.get('/api/portfolios', (_req, res) => {
    res.json({ portfolios: [] });
  });

  app.get('/api/portfolios/:portfolioId', (req, res) => {
    res.json({ id: req.params.portfolioId, name: 'Test Portfolio' });
  });

  app.post('/api/portfolios', (req, res) => {
    res.status(201).json({ id: 'PORT0001', ...req.body });
  });

  app.put('/api/portfolios/:portfolioId', (req, res) => {
    res.json({ id: req.params.portfolioId, ...req.body });
  });

  app.patch('/api/portfolios/:portfolioId', (req, res) => {
    res.json({ id: req.params.portfolioId, ...req.body });
  });

  app.delete('/api/portfolios/:portfolioId', (req, res) => {
    res.status(204).send();
  });

  app.post('/auth/login', (_req, res) => {
    res.json({ token: 'mock-jwt' });
  });

  app.post('/auth/logout', (_req, res) => {
    res.json({ message: 'logged out' });
  });

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.get('/api/error', (_req, res) => {
    res.status(500).json({ error: 'internal error' });
  });

  return app;
}

describe('Audit Middleware', () => {
  let repo: InMemoryAuditLogRepository;

  beforeEach(() => {
    repo = new InMemoryAuditLogRepository();
  });

  describe('request logging', () => {
    it('should log GET requests', async () => {
      const app = createApp(repo);
      await request(app).get('/api/portfolios').expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.total).toBe(1);
      expect(result.data[0].action).toBe(AuditAction.INQUIRE);
    });

    it('should log POST requests as CREATE', async () => {
      const app = createApp(repo);
      await request(app)
        .post('/api/portfolios')
        .send({ name: 'New Portfolio' })
        .expect(201);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.total).toBe(1);
      expect(result.data[0].action).toBe(AuditAction.CREATE);
    });

    it('should log PUT requests as UPDATE', async () => {
      const app = createApp(repo);
      await request(app)
        .put('/api/portfolios/PORT0001')
        .send({ name: 'Updated' })
        .expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.total).toBe(1);
      expect(result.data[0].action).toBe(AuditAction.UPDATE);
    });

    it('should log PATCH requests as UPDATE', async () => {
      const app = createApp(repo);
      await request(app)
        .patch('/api/portfolios/PORT0001')
        .send({ name: 'Patched' })
        .expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.total).toBe(1);
      expect(result.data[0].action).toBe(AuditAction.UPDATE);
    });

    it('should log DELETE requests', async () => {
      const app = createApp(repo);
      await request(app).delete('/api/portfolios/PORT0001').expect(204);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.total).toBe(1);
      expect(result.data[0].action).toBe(AuditAction.DELETE);
    });
  });

  describe('auth action mapping', () => {
    it('should map /auth/login to LOGIN action', async () => {
      const app = createApp(repo);
      await request(app)
        .post('/auth/login')
        .send({ username: 'test', password: 'pass' })
        .expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.LOGIN);
    });

    it('should map /auth/logout to LOGOUT action', async () => {
      const app = createApp(repo);
      await request(app).post('/auth/logout').expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].action).toBe(AuditAction.LOGOUT);
    });
  });

  describe('status mapping', () => {
    it('should map 200 to SUCCESS', async () => {
      const app = createApp(repo);
      await request(app).get('/api/portfolios').expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].status).toBe(AuditStatus.SUCCESS);
    });

    it('should map 500 to FAILURE', async () => {
      const app = createApp(repo);
      await request(app).get('/api/error').expect(500);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].status).toBe(AuditStatus.FAILURE);
    });
  });

  describe('before/after image capture', () => {
    it('should capture before image for POST requests', async () => {
      const app = createApp(repo);
      await request(app)
        .post('/api/portfolios')
        .send({ name: 'Test Portfolio' })
        .expect(201);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].beforeImage).toBeDefined();
      const beforeImage = JSON.parse(result.data[0].beforeImage!);
      expect(beforeImage.name).toBe('Test Portfolio');
    });

    it('should capture after image for POST responses', async () => {
      const app = createApp(repo);
      await request(app)
        .post('/api/portfolios')
        .send({ name: 'Test Portfolio' })
        .expect(201);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].afterImage).toBeDefined();
      const afterImage = JSON.parse(result.data[0].afterImage!);
      expect(afterImage.id).toBe('PORT0001');
    });

    it('should not capture before/after images for GET requests', async () => {
      const app = createApp(repo);
      await request(app).get('/api/portfolios').expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].beforeImage).toBeUndefined();
      expect(result.data[0].afterImage).toBeUndefined();
    });
  });

  describe('exclude patterns', () => {
    it('should skip audit logging for excluded paths', async () => {
      const app = createApp(repo, [/\/health\b/]);
      await request(app).get('/health').expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.total).toBe(0);
    });

    it('should still log non-excluded paths', async () => {
      const app = createApp(repo, [/\/health\b/]);
      await request(app).get('/api/portfolios').expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.total).toBe(1);
    });
  });

  describe('user context', () => {
    it('should capture userId from authenticated request', async () => {
      const app = createApp(repo);
      await request(app).get('/api/portfolios').expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].userId).toBe('testuser');
    });

    it('should use anonymous when no user context', async () => {
      const app = express();
      app.use(express.json());
      const auditService = new AuditService(repo);
      app.use(createAuditMiddleware({ auditService }));
      app.get('/api/test', (_req, res) => res.json({ ok: true }));

      await request(app).get('/api/test').expect(200);
      await new Promise((r) => setTimeout(r, 50));
      const result = await repo.findMany({});
      expect(result.data[0].userId).toBe('anonymous');
    });
  });

  describe('error resilience', () => {
    it('should not crash the request if audit logging fails', async () => {
      const failingRepo = {
        insert: jest.fn().mockRejectedValue(new Error('DB down')),
        findMany: jest.fn(),
      };
      const service = new AuditService(failingRepo);
      const app = express();
      app.use(express.json());
      app.use(createAuditMiddleware({ auditService: service }));
      app.get('/api/test', (_req, res) => res.json({ ok: true }));

      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      const response = await request(app).get('/api/test');
      expect(response.status).toBe(200);
      expect(response.body).toEqual({ ok: true });
      await new Promise((r) => setTimeout(r, 50));
      consoleSpy.mockRestore();
    });
  });
});
