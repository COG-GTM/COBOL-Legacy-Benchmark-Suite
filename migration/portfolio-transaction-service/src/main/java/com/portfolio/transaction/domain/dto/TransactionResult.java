package com.portfolio.transaction.domain.dto;

import com.portfolio.transaction.domain.entity.Portfolio;

public class TransactionResult {

    private final boolean success;
    private final Portfolio portfolio;
    private final String errorMessage;

    private TransactionResult(boolean success, Portfolio portfolio, String errorMessage) {
        this.success = success;
        this.portfolio = portfolio;
        this.errorMessage = errorMessage;
    }

    public static TransactionResult success(Portfolio portfolio) {
        return new TransactionResult(true, portfolio, null);
    }

    public static TransactionResult failure(String errorMessage) {
        return new TransactionResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
