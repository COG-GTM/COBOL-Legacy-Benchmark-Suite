import express from 'express';
import { InMemoryAuditLogRepository } from './repositories/auditRepository';
import { AuditService } from './services/auditService';
import { createAuditMiddleware } from './middleware/audit';
import { createAuditLogsRouter } from './routes/auditLogs';

const app = express();
const PORT = process.env.PORT ?? 3000;

app.use(express.json());

const auditRepository = new InMemoryAuditLogRepository();
const auditService = new AuditService(auditRepository);

app.use(
  createAuditMiddleware({
    auditService,
    excludePatterns: [/\/health\b/],
  }),
);

app.use('/api/audit-logs', createAuditLogsRouter({ auditService }));

app.get('/health', (_req, res) => {
  res.json({ status: 'ok' });
});

app.listen(PORT, () => {
  console.log(`CLBS Backend listening on port ${PORT}`);
});

export default app;
