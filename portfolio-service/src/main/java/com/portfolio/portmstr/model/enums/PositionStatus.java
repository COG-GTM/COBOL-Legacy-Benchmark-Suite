package com.portfolio.portmstr.model.enums;

/**
 * Position status codes.
 * Mapped from COBOL POSREC.cpy POS-STATUS level-88 conditions.
 */
public enum PositionStatus {
    ACTIVE('A'),
    CLOSED('C'),
    PENDING('P');

    private final char code;

    PositionStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static PositionStatus fromCode(char code) {
        for (PositionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid position status code: " + code);
    }
}
