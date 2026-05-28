package com.clbs.portfolio.common;

import lombok.Getter;

/**
 * Transaction types from COBOL COMMON.cpy (TRANSACTION-TYPES)
 * and TRNREC.cpy (TRN-TYPE level 88s).
 */
@Getter
public enum TransactionType {

    BUY("BU"),
    SELL("SL"),
    TRANSFER("TR"),
    FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type code: " + code);
    }
}
