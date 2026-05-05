package com.portfolio.portmstr.model.enums;

/**
 * Client type codes.
 * Mapped from COBOL PORTFLIO.cpy PORT-CLIENT-TYPE level-88 conditions.
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
        for (ClientType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid client type code: " + code);
    }
}
