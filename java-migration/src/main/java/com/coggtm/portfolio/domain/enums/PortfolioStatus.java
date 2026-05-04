package com.coggtm.portfolio.domain.enums;

/**
 * Maps to COBOL PORT-STATUS 88-level conditions in PORTFLIO.cpy.
 */
public enum PortfolioStatus {
    A("Active"),
    C("Closed"),
    S("Suspended");

    private final String description;

    PortfolioStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
