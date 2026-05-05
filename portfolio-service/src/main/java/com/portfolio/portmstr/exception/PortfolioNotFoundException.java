package com.portfolio.portmstr.exception;

/**
 * Thrown when a portfolio record is not found.
 * Equivalent to COBOL VSAM file status '23' (PORT-NOT-FOUND).
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
