package com.coggtm.portfolio.domain.enums;

/**
 * Maps to COBOL AUD-TYPE 88-level conditions in AUDITLOG.cpy.
 */
public enum AuditType {
    TRAN("Transaction"),
    USER("User Action"),
    SYST("System Event");

    private final String description;

    AuditType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
