package com.portfolio.model.enums;

public enum TransactionStatus {
    PENDING('P'),
    DONE('D'),
    FAILED('F'),
    REVERSED('R');

    private final char code;

    TransactionStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static TransactionStatus fromCode(char code) {
        for (TransactionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status code: " + code);
    }
}
