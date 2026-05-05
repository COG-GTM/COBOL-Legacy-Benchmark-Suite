package com.portfolio.portmstr.model.enums;

/**
 * Audit action codes.
 * Mapped from COBOL AUDITLOG.cpy AUD-ACTION level-88 conditions.
 */
public enum AuditAction {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    INQUIRE("INQUIRE");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
