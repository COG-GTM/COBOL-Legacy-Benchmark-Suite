package com.clbs.portfolio.model;

/** Level-88 conditions on {@code TRN-STATUS PIC X(01)} in {@code TRNREC.cpy}. */
public enum TransactionStatus {

    /** {@code 88 TRN-STATUS-PEND VALUE 'P'}. */
    PENDING("P"),
    /** {@code 88 TRN-STATUS-DONE VALUE 'D'}. */
    DONE("D"),
    /** {@code 88 TRN-STATUS-FAIL VALUE 'F'}. */
    FAILED("F"),
    /** {@code 88 TRN-STATUS-REV VALUE 'R'}. */
    REVERSED("R");

    public static final int LENGTH = 1;

    private final String code;

    TransactionStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** The matching status, or {@code null} when the buffer holds an uncovered value. */
    public static TransactionStatus fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (TransactionStatus status : values()) {
            if (status.code.equals(stored)) {
                return status;
            }
        }
        return null;
    }
}
