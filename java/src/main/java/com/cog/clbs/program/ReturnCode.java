package com.cog.clbs.program;

/**
 * Standard mainframe batch return codes (RC 0-16).
 *
 * <p>Mirrors {@code WS-RETURN-CODES} in
 * {@code src/templates/error/error-handling.cbl}:
 * RC-SUCCESS(+0), RC-WARNING(+4), RC-ERROR(+8), RC-SEVERE(+12),
 * RC-CRITICAL(+16).
 */
public enum ReturnCode {
    SUCCESS(0),
    WARNING(4),
    ERROR(8),
    SEVERE(12),
    CRITICAL(16);

    private final int code;

    ReturnCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** Returns the more severe of two return codes (highest wins, as on z/OS). */
    public ReturnCode max(ReturnCode other) {
        return this.code >= other.code ? this : other;
    }

    public static ReturnCode fromCode(int code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Unknown return code: " + code);
    }
}
