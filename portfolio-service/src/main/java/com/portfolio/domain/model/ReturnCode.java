package com.portfolio.domain.model;

/**
 * Maps COBOL return code statuses from RTNCODE.cpy RC-STATUS.
 */
public enum ReturnCode {
    SUCCESS('S'),
    WARNING('W'),
    ERROR('E'),
    SEVERE('F');

    private final char code;

    ReturnCode(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ReturnCode fromCode(char code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) return rc;
        }
        throw new IllegalArgumentException("Unknown return code: " + code);
    }

    public static boolean isValid(char code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) return true;
        }
        return false;
    }
}
