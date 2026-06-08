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
}
