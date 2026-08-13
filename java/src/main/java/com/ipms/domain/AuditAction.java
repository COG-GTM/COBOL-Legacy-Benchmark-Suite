package com.ipms.domain;

/** AUD-ACTION level-88 values from AUDITLOG.cpy (8-char, space padded). */
public enum AuditAction {
    CREATE("CREATE  "),
    UPDATE("UPDATE  "),
    DELETE("DELETE  "),
    INQUIRE("INQUIRE "),
    LOGIN("LOGIN   "),
    LOGOUT("LOGOUT  "),
    STARTUP("STARTUP "),
    SHUTDOWN("SHUTDOWN");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AuditAction fromCode(String code) {
        String padded = code == null ? "" : String.format("%-8s", code.strip());
        for (AuditAction a : values()) {
            if (a.code.equals(padded)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown audit action: " + code);
    }
}
