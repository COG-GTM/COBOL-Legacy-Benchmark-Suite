package com.portfolio.domain.model;

/**
 * Maps COBOL 88-level values from POSREC.cpy POS-STATUS.
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
        for (PositionStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown position status code: " + code);
    }
}
