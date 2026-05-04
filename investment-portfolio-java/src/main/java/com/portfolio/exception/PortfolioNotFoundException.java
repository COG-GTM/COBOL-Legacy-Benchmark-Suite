package com.portfolio.exception;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(String portfolioId) {
        super("Portfolio not found: " + portfolioId);
    }
}
