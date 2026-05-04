package com.portfolio.domain.enums;

/**
 * Return codes from COBOL COMMON.cpy RETURN-CODES.
 * Preserves original numeric values for backward compatibility.
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

    public static ReturnCode fromCode(int code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Unknown return code: " + code);
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isError() {
        return this.code >= ERROR.code;
    }
}
