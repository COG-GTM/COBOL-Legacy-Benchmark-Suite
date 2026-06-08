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

    public static AuditAction fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Unknown audit action code: null");
        }
        String trimmed = code.trim();
        for (AuditAction a : values()) {
            if (a.code.equals(trimmed)) return a;
        }
        throw new IllegalArgumentException("Unknown audit action code: " + code);
    }

    public static boolean isValid(String code) {
        if (code == null) return false;
        String trimmed = code.trim();
        for (AuditAction a : values()) {
            if (a.code.equals(trimmed)) return true;
        }
        return false;
    }
}
