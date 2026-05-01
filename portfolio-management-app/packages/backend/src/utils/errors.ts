import { ErrorCategory, ErrorSeverity, type ApiError } from '../types/index.js';
import { v4 as uuidv4 } from 'uuid';

export class AppError extends Error {
  public readonly code: string;
  public readonly category: ErrorCategory;
  public readonly severity: ErrorSeverity;
  public readonly statusCode: number;
  public readonly details?: string;
  public readonly traceId: string;

  constructor(
    message: string,
    code: string,
    category: ErrorCategory,
    severity: ErrorSeverity,
    statusCode: number = 500,
    details?: string
  ) {
    super(message);
    this.name = 'AppError';
    this.code = code;
    this.category = category;
    this.severity = severity;
    this.statusCode = statusCode;
    this.details = details;
    this.traceId = uuidv4().substring(0, 16);
  }

  toApiError(): ApiError {
    return {
      code: this.code,
      message: this.message,
      category: this.category,
      severity: this.severity,
      details: this.details,
      traceId: this.traceId,
    };
  }
}

// Validation errors (from PORTVAL.cpy pattern)
export class ValidationError extends AppError {
  constructor(message: string, details?: string) {
    super(message, 'VL001', ErrorCategory.Validation, ErrorSeverity.Error, 400, details);
    this.name = 'ValidationError';
  }
}

export class NotFoundError extends AppError {
  constructor(resource: string, id: string) {
    super(`${resource} not found: ${id}`, 'VL002', ErrorCategory.Validation, ErrorSeverity.Warning, 404);
    this.name = 'NotFoundError';
  }
}

export class DuplicateError extends AppError {
  constructor(resource: string, id: string) {
    super(`Duplicate ${resource}: ${id}`, 'VL003', ErrorCategory.Validation, ErrorSeverity.Error, 409);
    this.name = 'DuplicateError';
  }
}

export class AuthError extends AppError {
  constructor(message: string = 'Authentication required') {
    super(message, 'SY001', ErrorCategory.System, ErrorSeverity.Error, 401);
    this.name = 'AuthError';
  }
}

export class ForbiddenError extends AppError {
  constructor(message: string = 'Access denied') {
    super(message, 'SY002', ErrorCategory.System, ErrorSeverity.Error, 403);
    this.name = 'ForbiddenError';
  }
}

export class ProcessingError extends AppError {
  constructor(message: string, details?: string) {
    super(message, 'PR001', ErrorCategory.Processing, ErrorSeverity.Severe, 500, details);
    this.name = 'ProcessingError';
  }
}
