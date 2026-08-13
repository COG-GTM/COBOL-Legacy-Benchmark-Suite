package com.ipms.domain;

/** AUD-TYPE level-88 values from AUDITLOG.cpy (TRAN, USER, SYST). */
public enum AuditType {
    TRANSACTION("TRAN"),
    USER_ACTION("USER"),
    SYSTEM_EVENT("SYST");

    private final String code;

    AuditType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AuditType fromCode(String code) {
        for (AuditType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown audit type: " + code);
    }
}
