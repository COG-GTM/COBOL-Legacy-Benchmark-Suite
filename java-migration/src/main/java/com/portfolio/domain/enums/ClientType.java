package com.portfolio.domain.enums;

/**
 * Client types from COBOL PORTFLIO.cpy level-88 conditions.
 * PORT-INDIVIDUAL VALUE 'I', PORT-CORPORATE VALUE 'C', PORT-TRUST VALUE 'T'.
 */
public enum ClientType {
    INDIVIDUAL('I'),
    CORPORATE('C'),
    TRUST('T');

    private final char code;

    ClientType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ClientType fromCode(char code) {
        for (ClientType ct : values()) {
            if (ct.code == code) {
                return ct;
            }
        }
        throw new IllegalArgumentException("Unknown client type code: " + code);
    }
}
