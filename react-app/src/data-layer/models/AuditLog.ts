/**
 * Audit Log Model
 * Migrated from: AUDITLOG.cpy
 * 
 * Represents audit trail records for tracking system and user activities.
 */

import { AuditType, AuditAction, AuditStatus } from '../types';

/**
 * Audit record header
 * From: AUDITLOG.cpy (AUD-HEADER)
 */
export interface AuditHeader {
  /** Audit timestamp (26 characters, ISO format) */
  timestamp: Date;
  /** System identifier (8 characters) */
  systemId: string;
  /** User identifier (8 characters) */
  userId: string;
  /** Program name (8 characters) */
  program: string;
  /** Terminal identifier (8 characters) */
  terminal: string;
}

/**
 * Audit key information
 * From: AUDITLOG.cpy (AUD-KEY-INFO)
 */
export interface AuditKeyInfo {
  /** Portfolio identifier (8 characters) */
  portfolioId: string;
  /** Account number (10 characters) */
  accountNumber: string;
}

/**
 * Complete Audit Log record
 * From: AUDITLOG.cpy
 */
export interface AuditLog {
  /** Audit header information */
  header: AuditHeader;
  /** Audit type: Transaction, User Action, or System Event */
  auditType: AuditType;
  /** Action performed */
  action: AuditAction;
  /** Status of the action */
  status: AuditStatus;
  /** Key information for the affected record */
  keyInfo: AuditKeyInfo;
  /** Record image before the action (up to 100 characters) */
  beforeImage: string;
  /** Record image after the action (up to 100 characters) */
  afterImage: string;
  /** Audit message (up to 100 characters) */
  message: string;
}

/**
 * Audit log creation request
 */
export interface CreateAuditLogRequest {
  systemId: string;
  userId: string;
  program: string;
  terminal?: string;
  auditType: AuditType;
  action: AuditAction;
  status: AuditStatus;
  portfolioId?: string;
  accountNumber?: string;
  beforeImage?: string;
  afterImage?: string;
  message: string;
}

/**
 * Audit log search criteria
 */
export interface AuditLogSearchCriteria {
  timestampFrom?: Date;
  timestampTo?: Date;
  systemId?: string;
  userId?: string;
  program?: string;
  auditType?: AuditType;
  action?: AuditAction;
  status?: AuditStatus;
  portfolioId?: string;
  accountNumber?: string;
}

/**
 * Audit log summary for list views
 */
export interface AuditLogSummary {
  timestamp: Date;
  userId: string;
  program: string;
  auditType: AuditType;
  action: AuditAction;
  status: AuditStatus;
  message: string;
}

/**
 * Audit log page result
 * Used for paginated audit queries
 */
export interface AuditLogPage {
  records: AuditLog[];
  totalCount: number;
  pageNumber: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Audit statistics for reporting
 */
export interface AuditStatistics {
  periodStart: Date;
  periodEnd: Date;
  totalRecords: number;
  byType: Record<AuditType, number>;
  byAction: Record<AuditAction, number>;
  byStatus: Record<AuditStatus, number>;
  byUser: Record<string, number>;
  byProgram: Record<string, number>;
}

/**
 * Factory function to create a default AuditLog object
 */
export function createDefaultAuditLog(): AuditLog {
  return {
    header: {
      timestamp: new Date(),
      systemId: '',
      userId: '',
      program: '',
      terminal: '',
    },
    auditType: AuditType.USER_ACTION,
    action: AuditAction.INQUIRE,
    status: AuditStatus.SUCCESS,
    keyInfo: {
      portfolioId: '',
      accountNumber: '',
    },
    beforeImage: '',
    afterImage: '',
    message: '',
  };
}

/**
 * Create an audit log entry for a transaction
 */
export function createTransactionAuditLog(
  userId: string,
  program: string,
  action: AuditAction,
  status: AuditStatus,
  portfolioId: string,
  message: string,
  beforeImage?: string,
  afterImage?: string
): CreateAuditLogRequest {
  return {
    systemId: 'PORTMGMT',
    userId,
    program,
    auditType: AuditType.TRANSACTION,
    action,
    status,
    portfolioId,
    beforeImage,
    afterImage,
    message,
  };
}

/**
 * Create an audit log entry for a user action
 */
export function createUserActionAuditLog(
  userId: string,
  program: string,
  action: AuditAction,
  status: AuditStatus,
  message: string
): CreateAuditLogRequest {
  return {
    systemId: 'PORTMGMT',
    userId,
    program,
    auditType: AuditType.USER_ACTION,
    action,
    status,
    message,
  };
}

/**
 * Create an audit log entry for a system event
 */
export function createSystemEventAuditLog(
  program: string,
  action: AuditAction,
  status: AuditStatus,
  message: string
): CreateAuditLogRequest {
  return {
    systemId: 'PORTMGMT',
    userId: 'SYSTEM',
    program,
    auditType: AuditType.SYSTEM_EVENT,
    action,
    status,
    message,
  };
}

/**
 * Get audit type display name
 */
export function getAuditTypeDisplayName(type: AuditType): string {
  switch (type) {
    case AuditType.TRANSACTION:
      return 'Transaction';
    case AuditType.USER_ACTION:
      return 'User Action';
    case AuditType.SYSTEM_EVENT:
      return 'System Event';
    default:
      return 'Unknown';
  }
}

/**
 * Get audit action display name
 */
export function getAuditActionDisplayName(action: AuditAction): string {
  switch (action) {
    case AuditAction.CREATE:
      return 'Create';
    case AuditAction.UPDATE:
      return 'Update';
    case AuditAction.DELETE:
      return 'Delete';
    case AuditAction.INQUIRE:
      return 'Inquire';
    case AuditAction.LOGIN:
      return 'Login';
    case AuditAction.LOGOUT:
      return 'Logout';
    case AuditAction.STARTUP:
      return 'Startup';
    case AuditAction.SHUTDOWN:
      return 'Shutdown';
    default:
      return 'Unknown';
  }
}

/**
 * Get audit status display name
 */
export function getAuditStatusDisplayName(status: AuditStatus): string {
  switch (status) {
    case AuditStatus.SUCCESS:
      return 'Success';
    case AuditStatus.FAILURE:
      return 'Failure';
    case AuditStatus.WARNING:
      return 'Warning';
    default:
      return 'Unknown';
  }
}
