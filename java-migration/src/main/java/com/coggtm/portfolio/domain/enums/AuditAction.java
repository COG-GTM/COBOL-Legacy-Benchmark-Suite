package com.coggtm.portfolio.domain.enums;

/**
 * Maps to COBOL AUD-ACTION 88-level conditions in AUDITLOG.cpy.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    INQUIRE,
    LOGIN,
    LOGOUT,
    STARTUP,
    SHUTDOWN
}
