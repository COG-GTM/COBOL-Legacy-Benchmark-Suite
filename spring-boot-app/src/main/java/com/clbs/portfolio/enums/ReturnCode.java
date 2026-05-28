package com.clbs.portfolio.enums;

public enum ReturnCode {
    SUCCESS(0),
    WARNING(4),
    ERROR(8),
    SEVERE(12),
    TERMINAL(16);

    private final int code;

    ReturnCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ReturnCode fromCode(int code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Unknown ReturnCode: " + code);
    }
}
