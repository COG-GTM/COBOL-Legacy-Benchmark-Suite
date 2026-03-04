package com.cobolbenchmark.model;

/**
 * Transaction Status enum - from TRNREC.cpy level-88 conditions.
 * 88 TRN-STATUS-PEND VALUE 'P'.
 * 88 TRN-STATUS-DONE VALUE 'D'.
 * 88 TRN-STATUS-FAIL VALUE 'F'.
 * 88 TRN-STATUS-REV  VALUE 'R'.
 */
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
        for (TransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status code: " + code);
    }
}
