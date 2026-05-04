package com.portfolio.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
public class TransactionRecord {

    @Id
    @Column(name = "transaction_id", length = 20, nullable = false)
    private String transactionId;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "transaction_type", length = 2, nullable = false)
    private String transactionType;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "process_date", nullable = false)
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8, nullable = false)
    private String processUser;

    public TransactionRecord() {}

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

    public LocalDateTime getProcessDate() { return processDate; }
    public void setProcessDate(LocalDateTime processDate) { this.processDate = processDate; }

    public String getProcessUser() { return processUser; }
    public void setProcessUser(String processUser) { this.processUser = processUser; }

    public boolean isBuy() { return "BU".equals(transactionType); }
    public boolean isSell() { return "SL".equals(transactionType); }
    public boolean isTransfer() { return "TR".equals(transactionType); }
    public boolean isFee() { return "FE".equals(transactionType); }

    public boolean isPending() { return "P".equals(status); }
    public boolean isDone() { return "D".equals(status); }
    public boolean isFailed() { return "F".equals(status); }
    public boolean isReversed() { return "R".equals(status); }

    public String getTransactionTypeDisplay() {
        return switch (transactionType) {
            case "BU" -> "Buy";
            case "SL" -> "Sell";
            case "TR" -> "Transfer";
            case "FE" -> "Fee";
            default -> transactionType;
        };
    }
}
