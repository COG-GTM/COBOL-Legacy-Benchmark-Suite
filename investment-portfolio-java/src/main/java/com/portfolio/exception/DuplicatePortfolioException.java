package com.portfolio.exception;

public class DuplicatePortfolioException extends RuntimeException {

    public DuplicatePortfolioException(String portfolioId) {
        super("Duplicate portfolio ID: " + portfolioId);
    }
}
