import { Response } from 'express';
import { MAX_IMAGE_SIZE } from '../types/audit';

/**
 * Wraps res.json to capture the response body for audit after-image.
 * Returns a function to retrieve the captured body.
 */
export function captureResponseBody(res: Response): () => string | undefined {
  let capturedBody: string | undefined;
  const originalJson = res.json.bind(res);

  res.json = function (body?: unknown): Response {
    try {
      const serialized = JSON.stringify(body);
      if (serialized && serialized.length <= MAX_IMAGE_SIZE) {
        capturedBody = serialized;
      } else if (serialized) {
        capturedBody = serialized.substring(0, MAX_IMAGE_SIZE) + '...[truncated]';
      }
    } catch {
      capturedBody = undefined;
    }
    return originalJson(body);
  };

  return () => capturedBody;
}

/** Fields to redact from audit log images */
const SENSITIVE_FIELDS = [
  'password',
  'token',
  'secret',
  'authorization',
  'creditCard',
  'ssn',
  'pin',
];

/** Redacts sensitive fields from an object before logging */
export function redactSensitiveFields(obj: unknown): unknown {
  if (obj === null || obj === undefined) {
    return obj;
  }
  if (typeof obj !== 'object') {
    return obj;
  }
  if (Array.isArray(obj)) {
    return obj.map(redactSensitiveFields);
  }
  const redacted: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj as Record<string, unknown>)) {
    if (SENSITIVE_FIELDS.some((field) => key.toLowerCase().includes(field))) {
      redacted[key] = '[REDACTED]';
    } else if (typeof value === 'object' && value !== null) {
      redacted[key] = redactSensitiveFields(value);
    } else {
      redacted[key] = value;
    }
  }
  return redacted;
}

/** Safely serializes a request body for the before-image, with redaction and size limits */
export function serializeForAudit(body: unknown): string | undefined {
  if (body === null || body === undefined) {
    return undefined;
  }
  if (typeof body === 'object' && Object.keys(body as object).length === 0) {
    return undefined;
  }
  try {
    const redacted = redactSensitiveFields(body);
    const serialized = JSON.stringify(redacted);
    if (serialized.length <= MAX_IMAGE_SIZE) {
      return serialized;
    }
    return serialized.substring(0, MAX_IMAGE_SIZE) + '...[truncated]';
  } catch {
    return undefined;
  }
}
