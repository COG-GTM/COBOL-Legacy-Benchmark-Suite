package com.portfolio.exception;

public class DuplicatePortfolioException extends RuntimeException {

    public DuplicatePortfolioException(String portfolioId) {
        super("Portfolio ID already exists: " + portfolioId);
    }
}
