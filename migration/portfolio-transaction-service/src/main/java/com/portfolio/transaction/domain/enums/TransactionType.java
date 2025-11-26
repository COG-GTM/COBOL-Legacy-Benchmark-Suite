package com.portfolio.transaction.domain.enums;

import com.portfolio.transaction.exception.InvalidTransactionTypeException;

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
        if (code == null) {
            throw new InvalidTransactionTypeException(null);
        }
        for (TransactionType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new InvalidTransactionTypeException(code);
    }
}
