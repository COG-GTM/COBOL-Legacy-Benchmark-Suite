package com.coggtm.portfolio.domain.enums;

/**
 * Maps to COBOL TRN-TYPE 88-level conditions in TRNREC.cpy.
 */
public enum TransactionType {
    BU("Buy"),
    SL("Sell"),
    TR("Transfer"),
    FE("Fee");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
