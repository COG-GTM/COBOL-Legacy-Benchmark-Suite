package com.coggtm.portfolio.domain.enums;

/**
 * Maps to COBOL TRN-STATUS 88-level conditions in TRNREC.cpy.
 */
public enum TransactionStatus {
    P("Pending"),
    D("Done"),
    F("Failed"),
    R("Reversed");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
