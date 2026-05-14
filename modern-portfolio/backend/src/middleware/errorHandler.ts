// Centralized error handling (replaces ERRPROC.cbl + ERRHNDL.cbl)
import { Request, Response, NextFunction } from 'express';

interface AppError {
  name: string;
  message: string;
  code?: string;
  category?: string;
  severity?: number;
  statusCode?: number;
  details?: string;
}

export function errorHandler(
  err: AppError,
  _req: Request,
  res: Response,
  _next: NextFunction
): void {
  const statusCode = err.statusCode || 500;
  const category = err.category || 'SY';
  const severity = err.severity ?? 8;
  const code = err.code || 'SY99';

  console.error(`[${category}] ${err.name}: ${err.message}`, {
    code,
    severity,
    details: err.details,
  });

  res.status(statusCode).json({
    success: false,
    error: {
      code,
      message: err.message,
      category,
      severity,
      details: err.details,
    },
  });
}
