package com.coggtm.portfolio.domain.enums;

/**
 * Maps to DB2 RISK_LEVEL column in PORTFOLIO_MASTER.
 */
public enum RiskLevel {
    L("Low"),
    M("Medium"),
    H("High");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
