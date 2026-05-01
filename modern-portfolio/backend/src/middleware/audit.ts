// Audit middleware (replaces AUDPROC.cbl)
import { Request, Response, NextFunction } from 'express';

export function auditMiddleware(req: Request, _res: Response, next: NextFunction): void {
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) {
    const timestamp = new Date().toISOString();
    console.log(`[AUDIT] ${timestamp} ${req.method} ${req.path} user=${(req as unknown as Record<string, unknown>).userId || 'anonymous'}`);
  }
  next();
}
