package com.portfolio.domain.model;

/**
 * Maps COBOL 88-level values from TRNREC.cpy TRN-STATUS.
 */
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
        for (TransactionStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown transaction status code: " + code);
    }

    public static boolean isValid(char code) {
        for (TransactionStatus s : values()) {
            if (s.code == code) return true;
        }
        return false;
    }
}
