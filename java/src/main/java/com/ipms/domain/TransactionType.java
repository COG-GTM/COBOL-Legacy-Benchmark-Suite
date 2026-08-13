package com.ipms.domain;

/** TRN-TYPE level-88 values from TRNREC.cpy / COMMON.cpy. */
public enum TransactionType {
    BUY("BU"),
    SELL("SL"),
    TRANSFER("TR"),
    FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + code);
    }
}
