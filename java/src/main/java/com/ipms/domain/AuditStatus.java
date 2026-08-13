package com.ipms.domain;

/** AUD-STATUS level-88 values from AUDITLOG.cpy (SUCC, FAIL, WARN). */
public enum AuditStatus {
    SUCCESS("SUCC"),
    FAILURE("FAIL"),
    WARNING("WARN");

    private final String code;

    AuditStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AuditStatus fromCode(String code) {
        for (AuditStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown audit status: " + code);
    }
}
