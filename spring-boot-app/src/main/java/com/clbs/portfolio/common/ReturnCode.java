package com.clbs.portfolio.common;

import lombok.Getter;

/**
 * Return codes from COBOL COMMON.cpy and ERRHAND.cpy (ERR-RETURN-CODES).
 */
@Getter
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

    public static ReturnCode fromCode(int code) {
        for (ReturnCode rc : values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Unknown return code: " + code);
    }
}
