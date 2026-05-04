package com.portfolio.domain.enums;

/**
 * Error types from COBOL RETHND.cpy level-88 conditions.
 * ERR-VALIDATION 'V', ERR-PROCESSING 'P', ERR-DATABASE 'D',
 * ERR-FILE 'F', ERR-SECURITY 'S'.
 */
public enum ErrorType {
    VALIDATION('V'),
    PROCESSING('P'),
    DATABASE('D'),
    FILE('F'),
    SECURITY('S');

    private final char code;

    ErrorType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ErrorType fromCode(char code) {
        for (ErrorType et : values()) {
            if (et.code == code) {
                return et;
            }
        }
        throw new IllegalArgumentException("Unknown error type code: " + code);
    }
}
