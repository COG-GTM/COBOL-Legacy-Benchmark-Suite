package com.coggtm.portfolio.domain.enums;

/**
 * Maps to investment type validation in PORTVALD.cbl.
 */
public enum InvestmentType {
    STK("Stock"),
    BND("Bond"),
    MMF("Money Market Fund"),
    ETF("Exchange-Traded Fund");

    private final String description;

    InvestmentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
