package com.portfolio.transaction.domain.dto;

import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.entity.Transaction;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long transactionId;
    private String status;
    private String errorMessage;
    private PortfolioSummary portfolioSummary;
    private LocalDateTime processedAt;

    public TransactionResponse() {
    }

    public static TransactionResponse success(Transaction transaction, Portfolio portfolio) {
        TransactionResponse response = new TransactionResponse();
        response.transactionId = transaction.getId();
        response.status = "PROCESSED";
        response.processedAt = transaction.getProcessedAt();
        response.portfolioSummary = new PortfolioSummary(portfolio);
        return response;
    }

    public static TransactionResponse failure(Transaction transaction, String errorMessage) {
        TransactionResponse response = new TransactionResponse();
        response.transactionId = transaction.getId();
        response.status = "REJECTED";
        response.errorMessage = errorMessage;
        response.processedAt = LocalDateTime.now();
        return response;
    }

    public boolean isSuccess() {
        return "PROCESSED".equals(status);
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public PortfolioSummary getPortfolioSummary() {
        return portfolioSummary;
    }

    public void setPortfolioSummary(PortfolioSummary portfolioSummary) {
        this.portfolioSummary = portfolioSummary;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
