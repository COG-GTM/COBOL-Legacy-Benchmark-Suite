package com.clbs.portfolio.domain.enums;

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
        for (TransactionType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type code: " + code);
    }
}
