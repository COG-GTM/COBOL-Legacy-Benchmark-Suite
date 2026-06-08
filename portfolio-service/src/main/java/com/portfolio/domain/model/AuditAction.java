package com.portfolio.domain.model;

/**
 * Maps COBOL 88-level values from AUDITLOG.cpy AUD-ACTION.
 */
public enum AuditAction {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    INQUIRE("INQUIRE"),
    LOGIN("LOGIN"),
    LOGOUT("LOGOUT"),
    STARTUP("STARTUP"),
    SHUTDOWN("SHUTDOWN");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
