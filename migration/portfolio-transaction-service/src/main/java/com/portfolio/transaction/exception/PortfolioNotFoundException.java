package com.portfolio.transaction.exception;

public class PortfolioNotFoundException extends TransactionException {

    public PortfolioNotFoundException(String portfolioId) {
        super("ERR_PORTFOLIO_NOT_FOUND", "Invalid Portfolio ID: " + portfolioId);
    }
}
