package com.portfolio.domain.model;

/**
 * Maps COBOL return codes from ERRHAND.cpy ERR-RETURN-CODES.
 */
public enum ErrorSeverity {
    SUCCESS(0),
    WARNING(4),
    ERROR(8),
    SEVERE(12),
    TERMINAL(16);

    private final int code;

    ErrorSeverity(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ErrorSeverity fromCode(int code) {
        for (ErrorSeverity s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown error severity code: " + code);
    }

    public static boolean isValid(int code) {
        for (ErrorSeverity s : values()) {
            if (s.code == code) return true;
        }
        return false;
    }
}
