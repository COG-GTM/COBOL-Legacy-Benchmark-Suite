package com.portfolio.model.enums;

/**
 * Transaction status codes.
 * Migrated from: COBOL level-88 conditions in TRNREC.cpy.
 * P=Processed, F=Failed, R=Reversed
 */
public enum TransactionStatus {
    PROCESSED("P", "Processed"),
    FAILED("F", "Failed"),
    REVERSED("R", "Reversed");

    private final String code;
    private final String description;

    TransactionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status code: " + code);
    }
}
