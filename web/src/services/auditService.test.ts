import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  __resetAuditLog,
  getAuditRecords,
  recordAuditEvent,
  subscribeToAudit,
} from './auditService';

afterEach(() => __resetAuditLog());

describe('auditService', () => {
  it('records an event with copybook-mapped fields', () => {
    const record = recordAuditEvent({
      userId: 'ADMIN001',
      action: 'LOGIN',
      status: 'SUCC',
      message: 'User authenticated',
      program: 'SECMGR',
    });

    expect(record.systemId).toBe('CLBSWEB');
    expect(record.type).toBe('USER');
    expect(getAuditRecords()).toHaveLength(1);
  });

  it('defaults an empty user id to ANON', () => {
    const record = recordAuditEvent({
      userId: '',
      action: 'LOGIN',
      status: 'FAIL',
      message: 'bad',
      program: 'SECMGR',
    });
    expect(record.userId).toBe('ANON');
  });

  it('notifies subscribers and supports unsubscribe', () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToAudit(listener);
    expect(listener).toHaveBeenCalledTimes(1); // initial snapshot

    recordAuditEvent({
      userId: 'READ0001',
      action: 'LOGOUT',
      status: 'SUCC',
      message: 'bye',
      program: 'SECMGR',
    });
    expect(listener).toHaveBeenCalledTimes(2);

    unsubscribe();
    recordAuditEvent({
      userId: 'READ0001',
      action: 'LOGIN',
      status: 'SUCC',
      message: 'hi',
      program: 'SECMGR',
    });
    expect(listener).toHaveBeenCalledTimes(2);
  });
});
