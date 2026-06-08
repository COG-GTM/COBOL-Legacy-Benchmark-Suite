package com.portfolio.domain.model;

/**
 * Maps COBOL 88-level values from TRNREC.cpy TRN-TYPE.
 */
public enum TransactionType {
    BUY("BU"),
    SELL("SL"),
    TRANSFER("TR"),
    FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType t : values()) {
            if (t.code.equals(code)) return t;
        }
        throw new IllegalArgumentException("Unknown transaction type code: " + code);
    }

    public static boolean isValid(String code) {
        for (TransactionType t : values()) {
            if (t.code.equals(code)) return true;
        }
        return false;
    }
}
