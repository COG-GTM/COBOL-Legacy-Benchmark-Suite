package com.portfolio.entity;

public enum ErrorType {
    SYSTEM('S'),
    APPLICATION('A'),
    DATA('D');

    private final char code;

    ErrorType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ErrorType fromCode(char code) {
        for (ErrorType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorType code: " + code);
    }
}
