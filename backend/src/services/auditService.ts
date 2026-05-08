import { v4 as uuidv4 } from 'uuid';
import {
  AuditLogEntry,
  AuditLogQuery,
  AuditType,
  AuditAction,
  AuditStatus,
  PaginatedResult,
  HTTP_METHOD_TO_ACTION,
  AUTH_ACTION_PATTERNS,
  SYSTEM_ID,
} from '../types/audit';
import { IAuditLogRepository } from '../repositories/auditRepository';

export interface AuditEventInput {
  method: string;
  path: string;
  statusCode: number;
  userId: string;
  terminal: string;
  portfolioId?: string;
  accountNo?: string;
  beforeImage?: string;
  afterImage?: string;
}

export class AuditService {
  constructor(private readonly repository: IAuditLogRepository) {}

  async log(input: AuditEventInput): Promise<void> {
    const entry = this.buildEntry(input);
    try {
      await this.repository.insert(entry);
    } catch (err) {
      console.error('Audit log write failed (fallback to console):', JSON.stringify(entry), err);
    }
  }

  async query(filters: AuditLogQuery): Promise<PaginatedResult<AuditLogEntry>> {
    return this.repository.findMany(filters);
  }

  private buildEntry(input: AuditEventInput): AuditLogEntry {
    return {
      id: uuidv4(),
      timestamp: new Date().toISOString(),
      systemId: SYSTEM_ID,
      userId: input.userId,
      resource: input.path,
      terminal: input.terminal,
      type: this.resolveType(input.path),
      action: this.resolveAction(input.method, input.path),
      status: this.resolveStatus(input.statusCode),
      portfolioId: input.portfolioId,
      accountNo: input.accountNo,
      beforeImage: input.beforeImage,
      afterImage: input.afterImage,
      message: `${input.method} ${input.path} → ${input.statusCode}`,
    };
  }

  private resolveAction(method: string, path: string): AuditAction {
    for (const { pattern, action } of AUTH_ACTION_PATTERNS) {
      if (pattern.test(path)) {
        return action;
      }
    }
    return HTTP_METHOD_TO_ACTION[method.toUpperCase()] ?? AuditAction.INQUIRE;
  }

  private resolveType(path: string): AuditType {
    for (const { pattern } of AUTH_ACTION_PATTERNS) {
      if (pattern.test(path)) {
        return AuditType.USER_ACTION;
      }
    }
    return AuditType.TRANSACTION;
  }

  private resolveStatus(statusCode: number): AuditStatus {
    if (statusCode >= 200 && statusCode < 400) {
      return AuditStatus.SUCCESS;
    }
    if (statusCode >= 400 && statusCode < 500) {
      return AuditStatus.WARNING;
    }
    return AuditStatus.FAILURE;
  }
}
