/**
 * Security Manager.
 * Migrated from: src/programs/online/SECMGR.cbl
 *
 * Validates user credentials, authorizes resource access, and logs
 * security events.  Translates to Express middleware.
 */

import { Request, Response, NextFunction } from 'express';
import { ReturnCode } from '../../types';

/** Simple in-memory user store for demonstration. */
interface UserEntry {
  userId: string;
  password: string;
  roles: string[];
}

const USERS: UserEntry[] = [
  { userId: 'ADMIN', password: 'admin', roles: ['ADMIN', 'USER'] },
  { userId: 'USER01', password: 'user01', roles: ['USER'] },
  { userId: 'VIEWER', password: 'viewer', roles: ['VIEWER'] },
];

/** Resource access matrix. */
const ACCESS_MATRIX: Record<string, string[]> = {
  '/api/menu': ['ADMIN', 'USER', 'VIEWER'],
  '/api/portfolio': ['ADMIN', 'USER', 'VIEWER'],
  '/api/history': ['ADMIN', 'USER', 'VIEWER'],
  '/api/portfolio/create': ['ADMIN', 'USER'],
  '/api/portfolio/update': ['ADMIN', 'USER'],
  '/api/portfolio/delete': ['ADMIN'],
};

export class SecurityManager {
  /** 1000-VALIDATE – validate user credentials. */
  validate(userId: string, password: string): { rc: number; roles: string[] } {
    const user = USERS.find(
      (u) => u.userId === userId && u.password === password,
    );
    if (!user) {
      return { rc: ReturnCode.Error, roles: [] };
    }
    return { rc: ReturnCode.Success, roles: user.roles };
  }

  /** 2000-AUTHORIZE – check if user role has access to a resource. */
  authorize(roles: string[], resource: string): number {
    const allowedRoles = ACCESS_MATRIX[resource];
    if (!allowedRoles) {
      return ReturnCode.Success; // no restrictions defined
    }
    const hasAccess = roles.some((r) => allowedRoles.includes(r));
    return hasAccess ? ReturnCode.Success : ReturnCode.Error;
  }

  /** 3000-AUDIT – log security event (placeholder). */
  auditSecurityEvent(userId: string, action: string, resource: string, result: string): void {
    console.log(`[SECURITY] user=${userId} action=${action} resource=${resource} result=${result}`);
  }
}

/**
 * Express middleware for authentication.
 * Reads Basic auth or X-User-Id / X-Password headers.
 */
export function authMiddleware(secMgr: SecurityManager) {
  return (req: Request, res: Response, next: NextFunction): void => {
    const userId = (req.headers['x-user-id'] as string) || 'VIEWER';
    const password = (req.headers['x-password'] as string) || 'viewer';

    const { rc, roles } = secMgr.validate(userId, password);
    if (rc !== ReturnCode.Success) {
      secMgr.auditSecurityEvent(userId, 'LOGIN', req.path, 'FAIL');
      res.status(401).json({ error: 'Authentication failed' });
      return;
    }

    // Attach to request for downstream use
    (req as unknown as Record<string, unknown>)['userRoles'] = roles;
    (req as unknown as Record<string, unknown>)['userId'] = userId;

    // Authorization check
    const authRc = secMgr.authorize(roles, req.path);
    if (authRc !== ReturnCode.Success) {
      secMgr.auditSecurityEvent(userId, 'AUTHZ', req.path, 'FAIL');
      res.status(403).json({ error: 'Access denied' });
      return;
    }

    secMgr.auditSecurityEvent(userId, 'ACCESS', req.path, 'OK');
    next();
  };
}
