package com.portfolio.portmstr.model.enums;

/**
 * Transaction type codes.
 * Mapped from COBOL TRNREC.cpy TRN-TYPE level-88 conditions.
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
        throw new IllegalArgumentException("Invalid transaction type code: " + code);
    }
}
