package com.portfolio.model.enums;

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
        for (AuditType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown audit type code: " + code);
    }
}
