package com.coggtm.portfolio.domain.enums;

/**
 * Maps to COBOL PORT-CLIENT-TYPE 88-level conditions and DB2 ACCOUNT_TYPE column.
 */
public enum AccountType {
    IN("Individual"),
    CO("Corporate"),
    TR("Trust");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
