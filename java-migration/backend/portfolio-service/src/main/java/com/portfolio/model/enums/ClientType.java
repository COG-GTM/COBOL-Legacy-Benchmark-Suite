package com.portfolio.model.enums;

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
        throw new IllegalArgumentException("Unknown client type code: " + code);
    }
}
