package com.clbs.portfolio.enums;

public enum TransactionType {
    BU("BUY"),
    SL("SELL"),
    TR("TRANSFER"),
    FE("FEE");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + code);
    }
}
