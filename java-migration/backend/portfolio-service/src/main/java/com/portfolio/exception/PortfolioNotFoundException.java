package com.portfolio.exception;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(String message) {
        super(message);
    }

    public PortfolioNotFoundException(String portfolioId, boolean isPortfolioId) {
        super(isPortfolioId 
            ? "Portfolio not found with ID: " + portfolioId 
            : "Portfolio not found with account number: " + portfolioId);
    }
}
