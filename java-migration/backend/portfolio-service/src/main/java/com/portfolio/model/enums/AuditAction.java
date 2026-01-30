package com.portfolio.model.enums;

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
        for (AuditAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown audit action code: " + code);
    }
}
