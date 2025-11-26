package com.portfolio.transaction.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class TransactionRequest {

    @NotBlank(message = "Portfolio ID is required")
    @Size(max = 10)
    private String portfolioId;

    @NotBlank(message = "Transaction type is required")
    @Size(min = 2, max = 2)
    private String transactionType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private BigDecimal price;

    private BigDecimal amount;

    public TransactionRequest() {
    }

    public TransactionRequest(String portfolioId, String transactionType, 
                              BigDecimal quantity, BigDecimal price, BigDecimal amount) {
        this.portfolioId = portfolioId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.price = price;
        this.amount = amount;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
