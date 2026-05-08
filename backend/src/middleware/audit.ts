import { Request, Response, NextFunction } from 'express';
import onFinished from 'on-finished';
import { AuditService } from '../services/auditService';
import { captureResponseBody, serializeForAudit } from '../utils/responseCapture';

/** Extended request with auth context (set by upstream auth middleware) */
interface AuthenticatedRequest extends Request {
  user?: {
    userId: string;
    [key: string]: unknown;
  };
}

export interface AuditMiddlewareOptions {
  auditService: AuditService;
  /** URL patterns to skip audit logging (e.g., health checks) */
  excludePatterns?: RegExp[];
}

/**
 * Factory function that creates audit logging middleware.
 * Uses dependency injection — accepts an AuditService instance.
 *
 * Replicates SECMGR P300-LOG-ACCESS: captures timestamp, user ID,
 * terminal, program (resource), action, and before/after images.
 */
export function createAuditMiddleware(options: AuditMiddlewareOptions) {
  const { auditService, excludePatterns = [] } = options;

  return function auditMiddleware(
    req: AuthenticatedRequest,
    res: Response,
    next: NextFunction,
  ): void {
    const fullPath = req.baseUrl + req.path;

    if (excludePatterns.some((pattern) => pattern.test(fullPath))) {
      next();
      return;
    }

    const isMutation = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method);
    const beforeImage = isMutation ? serializeForAudit(req.body) : undefined;

    const getAfterImage = isMutation ? captureResponseBody(res) : () => undefined;

    const userId = req.user?.userId ?? 'anonymous';
    const terminal = req.ip ?? req.socket?.remoteAddress ?? 'unknown';

    const portfolioId =
      (req.params as Record<string, string>).portfolioId ??
      (req.params as Record<string, string>).id;
    const accountNo = (req.params as Record<string, string>).accountNo;

    onFinished(res, () => {
      auditService
        .log({
          method: req.method,
          path: fullPath,
          statusCode: res.statusCode,
          userId,
          terminal,
          portfolioId,
          accountNo,
          beforeImage,
          afterImage: getAfterImage(),
        })
        .catch((err) => {
          console.error('Audit middleware: failed to log entry', err);
        });
    });

    next();
  };
}
