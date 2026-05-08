import {
  AuditLogEntry,
  AuditLogQuery,
  PaginatedResult,
} from '../types/audit';

/** Repository interface for audit log persistence (dependency injection) */
export interface IAuditLogRepository {
  insert(entry: AuditLogEntry): Promise<void>;
  findMany(query: AuditLogQuery): Promise<PaginatedResult<AuditLogEntry>>;
}

const DEFAULT_LIMIT = 50;
const MAX_LIMIT = 100;

/** In-memory implementation for testing and development */
export class InMemoryAuditLogRepository implements IAuditLogRepository {
  private entries: AuditLogEntry[] = [];

  async insert(entry: AuditLogEntry): Promise<void> {
    this.entries.push(entry);
  }

  async findMany(query: AuditLogQuery): Promise<PaginatedResult<AuditLogEntry>> {
    let filtered = [...this.entries];

    if (query.userId) {
      filtered = filtered.filter((e) => e.userId === query.userId);
    }
    if (query.action) {
      filtered = filtered.filter((e) => e.action === query.action);
    }
    if (query.type) {
      filtered = filtered.filter((e) => e.type === query.type);
    }
    if (query.status) {
      filtered = filtered.filter((e) => e.status === query.status);
    }
    if (query.portfolioId) {
      filtered = filtered.filter((e) => e.portfolioId === query.portfolioId);
    }
    if (query.startDate) {
      filtered = filtered.filter((e) => e.timestamp >= query.startDate!);
    }
    if (query.endDate) {
      filtered = filtered.filter((e) => e.timestamp <= query.endDate!);
    }

    filtered.sort((a, b) => b.timestamp.localeCompare(a.timestamp));

    const total = filtered.length;
    const limit = Math.min(query.limit ?? DEFAULT_LIMIT, MAX_LIMIT);
    const offset = query.offset ?? 0;
    const data = filtered.slice(offset, offset + limit);

    return { data, total, limit, offset };
  }

  /** Test helper: clear all entries */
  clear(): void {
    this.entries = [];
  }

  /** Test helper: get raw entry count */
  count(): number {
    return this.entries.length;
  }
}
