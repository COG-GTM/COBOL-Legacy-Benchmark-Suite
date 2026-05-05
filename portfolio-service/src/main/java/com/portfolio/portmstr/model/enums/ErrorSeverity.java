package com.portfolio.portmstr.model.enums;

/**
 * Error severity levels.
 * Mapped from COBOL ERRHAND.cpy ERR-RETURN-CODES.
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
        for (ErrorSeverity severity : values()) {
            if (severity.code == code) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Invalid error severity code: " + code);
    }
}
