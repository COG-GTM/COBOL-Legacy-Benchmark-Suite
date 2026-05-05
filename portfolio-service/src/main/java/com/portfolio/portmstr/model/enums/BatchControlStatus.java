package com.portfolio.portmstr.model.enums;

/**
 * Batch control status codes.
 * Mapped from COBOL BCHCTL.cpy BCT-STATUS level-88 conditions.
 */
public enum BatchControlStatus {
    READY('R'),
    ACTIVE('A'),
    WAITING('W'),
    DONE('D'),
    ERROR('E');

    private final char code;

    BatchControlStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static BatchControlStatus fromCode(char code) {
        for (BatchControlStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid batch control status code: " + code);
    }
}
