package com.portfolio.exception;

/**
 * Exception thrown when a portfolio is not found.
 * Replaces: INQPORT.cbl P900-NOT-FOUND paragraph.
 */
public class PortfolioNotFoundException extends RuntimeException {

    private final String portfolioId;

    public PortfolioNotFoundException(String portfolioId) {
        super("Portfolio not found: " + portfolioId);
        this.portfolioId = portfolioId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }
}
