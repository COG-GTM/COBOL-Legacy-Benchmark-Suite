package com.clbs.portfolio.domain.enums;

public enum TransactionStatus {
    PENDING("P"),
    DONE("D"),
    FAILED("F"),
    REVERSED("R");

    private final String code;

    TransactionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status code: " + code);
    }
}
