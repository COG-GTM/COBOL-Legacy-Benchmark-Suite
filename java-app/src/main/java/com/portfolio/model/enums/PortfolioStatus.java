package com.portfolio.model.enums;

/**
 * Portfolio status codes.
 * Migrated from: COBOL level-88 conditions in PORTFLIO.cpy and db2-definitions.sql.
 * A=Active, C=Closed, S=Suspended
 */
public enum PortfolioStatus {
    ACTIVE("A", "Active"),
    CLOSED("C", "Closed"),
    SUSPENDED("S", "Suspended");

    private final String code;
    private final String description;

    PortfolioStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PortfolioStatus fromCode(String code) {
        for (PortfolioStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown portfolio status code: " + code);
    }
}
