package com.ipms.domain;

/** TRN-STATUS level-88 values from TRNREC.cpy (P=PENDING, D=DONE, F=FAILED, R=REVERSED). */
public enum TransactionStatus {
    PENDING("P"),
    DONE("D"),
    FAILED("F"),
    REVERSED("R");

    private final String code;

    TransactionStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status: " + code);
    }
}
