package com.clbs.portfolio.service;

/**
 * Audit sink, replacing the {@code CALL 'AUDPROC' USING AUDIT-RECORD} performed by
 * PORTTRAN's {@code 2310-WRITE-AUDIT-RECORD}.
 */
public interface AuditService {

    /**
     * Record an audit entry.
     *
     * @param record the populated audit record
     */
    void record(AuditRecord record);
}
