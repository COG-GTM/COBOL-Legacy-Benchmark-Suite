package com.portfolio.model.enums;

/**
 * Transaction type codes.
 * Migrated from: COBOL level-88 conditions in TRNREC.cpy.
 * BU=Buy, SL=Sell, TR=Transfer, FE=Fee
 */
public enum TransactionType {
    BUY("BU", "Buy"),
    SELL("SL", "Sell"),
    TRANSFER("TR", "Transfer"),
    FEE("FE", "Fee");

    private final String code;
    private final String description;

    TransactionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
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
