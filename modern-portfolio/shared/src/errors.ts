// Error types from ERRHAND.cpy and ERRPROC.cbl
import { ErrorCategory, ErrorSeverity } from './enums';

export class AppError extends Error {
  public readonly code: string;
  public readonly category: ErrorCategory;
  public readonly severity: ErrorSeverity;
  public readonly statusCode: number;
  public readonly details?: string;

  constructor(params: {
    message: string;
    code: string;
    category: ErrorCategory;
    severity: ErrorSeverity;
    statusCode?: number;
    details?: string;
  }) {
    super(params.message);
    this.name = 'AppError';
    this.code = params.code;
    this.category = params.category;
    this.severity = params.severity;
    this.statusCode = params.statusCode ?? 500;
    this.details = params.details;
  }
}

export class ValidationError extends AppError {
  constructor(message: string, details?: string) {
    super({
      message,
      code: 'VL01',
      category: ErrorCategory.VALIDATION,
      severity: ErrorSeverity.ERROR,
      statusCode: 400,
      details,
    });
    this.name = 'ValidationError';
  }
}

export class NotFoundError extends AppError {
  constructor(resource: string, id: string) {
    super({
      message: `${resource} not found: ${id}`,
      code: 'VL02',
      category: ErrorCategory.VALIDATION,
      severity: ErrorSeverity.WARNING,
      statusCode: 404,
    });
    this.name = 'NotFoundError';
  }
}

export class DuplicateError extends AppError {
  constructor(resource: string, id: string) {
    super({
      message: `${resource} already exists: ${id}`,
      code: 'VL03',
      category: ErrorCategory.VALIDATION,
      severity: ErrorSeverity.WARNING,
      statusCode: 409,
    });
    this.name = 'DuplicateError';
  }
}

export class AuthenticationError extends AppError {
  constructor(message = 'Authentication failed') {
    super({
      message,
      code: 'SY01',
      category: ErrorCategory.SYSTEM,
      severity: ErrorSeverity.ERROR,
      statusCode: 401,
    });
    this.name = 'AuthenticationError';
  }
}

export class AuthorizationError extends AppError {
  constructor(message = 'Access denied') {
    super({
      message,
      code: 'SY02',
      category: ErrorCategory.SYSTEM,
      severity: ErrorSeverity.ERROR,
      statusCode: 403,
    });
    this.name = 'AuthorizationError';
  }
}

export class ProcessingError extends AppError {
  constructor(message: string, details?: string) {
    super({
      message,
      code: 'PR01',
      category: ErrorCategory.PROCESSING,
      severity: ErrorSeverity.ERROR,
      statusCode: 500,
      details,
    });
    this.name = 'ProcessingError';
  }
}
