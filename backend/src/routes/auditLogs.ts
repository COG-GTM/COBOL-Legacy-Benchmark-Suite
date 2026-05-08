import { Router, Request, Response } from 'express';
import { AuditService } from '../services/auditService';
import { AuditAction, AuditType, AuditStatus, AuditLogQuery } from '../types/audit';

export interface AuditLogsRouterOptions {
  auditService: AuditService;
}

/**
 * Creates the audit logs query router.
 * GET /api/audit-logs — paginated, filtered audit log retrieval.
 */
export function createAuditLogsRouter(options: AuditLogsRouterOptions): Router {
  const { auditService } = options;
  const router = Router();

  router.get('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const query = buildQuery(req);
      const result = await auditService.query(query);
      res.json(result);
    } catch (err) {
      console.error('Error querying audit logs:', err);
      res.status(500).json({ error: 'Failed to query audit logs' });
    }
  });

  return router;
}

function buildQuery(req: Request): AuditLogQuery {
  const query: AuditLogQuery = {};

  if (typeof req.query.userId === 'string') {
    query.userId = req.query.userId;
  }
  if (typeof req.query.action === 'string' && isValidAction(req.query.action)) {
    query.action = req.query.action as AuditAction;
  }
  if (typeof req.query.type === 'string' && isValidType(req.query.type)) {
    query.type = req.query.type as AuditType;
  }
  if (typeof req.query.status === 'string' && isValidStatus(req.query.status)) {
    query.status = req.query.status as AuditStatus;
  }
  if (typeof req.query.portfolioId === 'string') {
    query.portfolioId = req.query.portfolioId;
  }
  if (typeof req.query.startDate === 'string') {
    query.startDate = req.query.startDate;
  }
  if (typeof req.query.endDate === 'string') {
    query.endDate = req.query.endDate;
  }
  if (typeof req.query.limit === 'string') {
    const parsed = parseInt(req.query.limit, 10);
    if (!isNaN(parsed) && parsed > 0) {
      query.limit = parsed;
    }
  }
  if (typeof req.query.offset === 'string') {
    const parsed = parseInt(req.query.offset, 10);
    if (!isNaN(parsed) && parsed >= 0) {
      query.offset = parsed;
    }
  }

  return query;
}

function isValidAction(value: string): boolean {
  return Object.values(AuditAction).includes(value as AuditAction);
}

function isValidType(value: string): boolean {
  return Object.values(AuditType).includes(value as AuditType);
}

function isValidStatus(value: string): boolean {
  return Object.values(AuditStatus).includes(value as AuditStatus);
}
