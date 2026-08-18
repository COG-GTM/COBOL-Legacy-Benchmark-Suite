package com.cog.clbs.error;

/**
 * Error severity levels.
 *
 * <p>Mirrors the ERR-SEVERITY 88-levels in {@code src/copybook/online/ERRHND.cpy}:
 * 'F' fatal, 'W' warning, 'I' informational.
 */
public enum ErrorSeverity {
    FATAL('F'),
    WARNING('W'),
    INFO('I');

    private final char code;

    ErrorSeverity(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ErrorSeverity fromCode(char code) {
        for (ErrorSeverity s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown severity code: " + code);
    }
}
