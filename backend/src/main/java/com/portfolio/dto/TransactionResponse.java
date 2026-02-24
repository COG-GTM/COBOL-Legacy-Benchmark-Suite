package com.portfolio.dto;

import com.portfolio.entity.TransactionHistory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Transaction response DTO - maps to BMS HISMAP history row fields.
 * Source: src/maps/INQSET.bms HISMAP definition (ROW1-ROW10)
 */
public class TransactionResponse {

    private String transactionId;
    private String portfolioId;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private String investmentId;
    private String transactionType;
    private String transactionTypeLabel;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private String currencyCode;
    private String status;

    public TransactionResponse() {
    }

    public static TransactionResponse fromEntity(TransactionHistory entity) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(entity.getTransactionId());
        response.setPortfolioId(entity.getPortfolioId());
        response.setTransactionDate(entity.getTransactionDate());
        response.setTransactionTime(entity.getTransactionTime());
        response.setInvestmentId(entity.getInvestmentId());
        response.setTransactionType(entity.getTransactionType());
        response.setTransactionTypeLabel(mapTransactionType(entity.getTransactionType()));
        response.setQuantity(entity.getQuantity());
        response.setPrice(entity.getPrice());
        response.setAmount(entity.getAmount());
        response.setCurrencyCode(entity.getCurrencyCode());
        response.setStatus(entity.getStatus());
        return response;
    }

    private static String mapTransactionType(String type) {
        return switch (type) {
            case "BU" -> "Buy";
            case "SL" -> "Sell";
            case "TR" -> "Transfer";
            case "FE" -> "Fee";
            default -> type;
        };
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public LocalTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalTime transactionTime) { this.transactionTime = transactionTime; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getTransactionTypeLabel() { return transactionTypeLabel; }
    public void setTransactionTypeLabel(String transactionTypeLabel) { this.transactionTypeLabel = transactionTypeLabel; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
