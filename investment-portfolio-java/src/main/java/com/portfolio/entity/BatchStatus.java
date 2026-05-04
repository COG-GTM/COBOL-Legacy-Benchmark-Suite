package com.portfolio.entity;

public enum BatchStatus {
    READY('R'),
    ACTIVE('A'),
    WAITING('W'),
    DONE('D'),
    ERROR('E');

    private final char code;

    BatchStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static BatchStatus fromCode(char code) {
        for (BatchStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown BatchStatus code: " + code);
    }
}
