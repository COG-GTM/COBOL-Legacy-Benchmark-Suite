package com.clbs.common.returncode;

/**
 * Mirrors the RETURN-CODES level-88 values from {@code COMMON.cpy}.
 * COBOL programs set RETURN-CODE to one of these severity levels.
 */
public enum ReturnCode {
    SUCCESS(0, 'S'),
    WARNING(4, 'W'),
    ERROR(8, 'E'),
    SEVERE(12, 'F'),
    CRITICAL(16, 'F');

    private final int code;
    private final char status;

    ReturnCode(int code, char status) {
        this.code = code;
        this.status = status;
    }

    public int code() {
        return code;
    }

    /** Maps to RC-STATUS (S=success, W=warning, E=error, F=severe/fatal). */
    public char status() {
        return status;
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
