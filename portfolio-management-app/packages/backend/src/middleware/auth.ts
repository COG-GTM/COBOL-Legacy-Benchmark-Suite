import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { AuthError, ForbiddenError } from '../utils/errors.js';
import type { JwtPayload } from '../types/index.js';

const JWT_SECRET = process.env.JWT_SECRET || 'portfolio-management-secret-key';

declare global {
  namespace Express {
    interface Request {
      user?: JwtPayload;
    }
  }
}

// Security check middleware — maps from SECMGR.cbl P100-VALIDATE-USER
export function authenticate(req: Request, _res: Response, next: NextFunction): void {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      throw new AuthError('No token provided');
    }

    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, JWT_SECRET) as JwtPayload;
    req.user = decoded;
    next();
  } catch (err) {
    if (err instanceof AuthError) {
      next(err);
    } else {
      next(new AuthError('Invalid or expired token'));
    }
  }
}

// Authorization check — maps from SECMGR.cbl P200-CHECK-AUTH
export function authorize(...roles: string[]) {
  return (req: Request, _res: Response, next: NextFunction): void => {
    if (!req.user) {
      return next(new AuthError());
    }
    if (roles.length > 0 && !roles.includes(req.user.role)) {
      return next(new ForbiddenError('Insufficient permissions'));
    }
    next();
  };
}

export function generateToken(payload: JwtPayload): string {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' });
}

export function verifyToken(token: string): JwtPayload {
  return jwt.verify(token, JWT_SECRET) as JwtPayload;
}
