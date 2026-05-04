package com.portfolio.entity;

public enum AuditStatus {
    SUCCESS("SUCC"),
    FAILURE("FAIL"),
    WARNING("WARN");

    private final String code;

    AuditStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AuditStatus fromCode(String code) {
        for (AuditStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown AuditStatus code: " + code);
    }
}
