package com.cobolbenchmark.model;

/**
 * Transaction Type enum - from TRNREC.cpy level-88 conditions.
 * 88 TRN-TYPE-BUY   VALUE 'BU'.
 * 88 TRN-TYPE-SELL  VALUE 'SL'.
 * 88 TRN-TYPE-TRANS VALUE 'TR'.
 * 88 TRN-TYPE-FEE   VALUE 'FE'.
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
        for (TransactionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type code: " + code);
    }
}
