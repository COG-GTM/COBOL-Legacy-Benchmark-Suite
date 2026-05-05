package com.portfolio.portmstr.exception;

/**
 * Thrown when attempting to create a portfolio that already exists.
 * Equivalent to COBOL VSAM file status '22' (PORT-DUP-KEY).
 */
public class DuplicatePortfolioException extends RuntimeException {

    private final String portfolioId;

    public DuplicatePortfolioException(String portfolioId) {
        super("Portfolio ID already exists: " + portfolioId);
        this.portfolioId = portfolioId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }
}
