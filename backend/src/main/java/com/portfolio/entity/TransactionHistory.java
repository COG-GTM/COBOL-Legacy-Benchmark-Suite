package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Transaction History entity - migrated from DB2 TRANSACTION_HISTORY table
 * and VSAM TRANHIST file.
 * Source: TRNREC copybook, src/database/db2/db2-definitions.sql
 *
 * Transaction types: 'BU'=Buy, 'SL'=Sell, 'TR'=Transfer, 'FE'=Fee
 * Status: 'P'=Processed, 'F'=Failed, 'R'=Reversed, 'D'=Done
 */
@Entity
@Table(name = "transaction_history")
public class TransactionHistory {

    @Id
    @Column(name = "transaction_id", length = 20)
    @NotBlank
    private String transactionId;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    @NotBlank
    private String portfolioId;

    @Column(name = "transaction_date", nullable = false)
    @NotNull
    private LocalDate transactionDate;

    @Column(name = "transaction_time", nullable = false)
    @NotNull
    private LocalTime transactionTime;

    @Column(name = "investment_id", length = 10, nullable = false)
    @NotBlank
    private String investmentId;

    @Column(name = "transaction_type", length = 2, nullable = false)
    @NotBlank
    private String transactionType;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    @NotNull
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    @NotNull
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "currency_code", length = 3, nullable = false)
    @NotBlank
    private String currencyCode = "USD";

    @Column(name = "status", length = 1, nullable = false)
    @NotBlank
    private String status = "P";

    @Column(name = "process_date", nullable = false)
    private LocalDateTime processDate = LocalDateTime.now();

    @Column(name = "process_user", length = 8, nullable = false)
    @NotBlank
    private String processUser;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", insertable = false, updatable = false)
    private PortfolioMaster portfolio;

    public TransactionHistory() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDateTime processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }

    public PortfolioMaster getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(PortfolioMaster portfolio) {
        this.portfolio = portfolio;
    }

    public boolean isBuy() {
        return "BU".equals(transactionType);
    }

    public boolean isSell() {
        return "SL".equals(transactionType);
    }

    public boolean isFee() {
        return "FE".equals(transactionType);
    }

    public boolean isTransfer() {
        return "TR".equals(transactionType);
    }
}
