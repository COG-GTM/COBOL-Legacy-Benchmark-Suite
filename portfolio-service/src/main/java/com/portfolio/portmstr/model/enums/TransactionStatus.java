package com.portfolio.portmstr.model.enums;

/**
 * Transaction status codes.
 * Mapped from COBOL TRNREC.cpy TRN-STATUS level-88 conditions.
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
        for (TransactionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid transaction status code: " + code);
    }
}
