package com.portfolio.domain.model;

/**
 * Maps COBOL 88-level values from AUDITLOG.cpy AUD-TYPE.
 */
public enum AuditType {
    TRANSACTION("TRAN"),
    USER_ACTION("USER"),
    SYSTEM_EVENT("SYST");

    private final String code;

    AuditType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AuditType fromCode(String code) {
        for (AuditType t : values()) {
            if (t.code.equals(code)) return t;
        }
        throw new IllegalArgumentException("Unknown audit type code: " + code);
    }

    public static boolean isValid(String code) {
        for (AuditType t : values()) {
            if (t.code.equals(code)) return true;
        }
        return false;
    }
}
