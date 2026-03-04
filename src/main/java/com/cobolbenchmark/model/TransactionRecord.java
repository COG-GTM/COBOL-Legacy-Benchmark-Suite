package com.cobolbenchmark.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Transaction Record - migrated from TRNREC.cpy.
 * Represents a financial transaction with COMP-3 amount fields.
 */
@Entity
@Table(name = "TRANSACTION_HISTORY")
public class TransactionRecord {

    /** TRN-TRANSACTION-ID PIC X(20) */
    @Id
    @Column(name = "TRANSACTION_ID", length = 20, nullable = false)
    private String transactionId;

    /** TRN-PORTFOLIO-ID PIC X(8) */
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    /** TRN-DATE */
    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    /** TRN-TIME */
    @Column(name = "TRANSACTION_TIME", nullable = false)
    private LocalTime transactionTime;

    /** TRN-INVESTMENT-ID PIC X(10) */
    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    /** TRN-TYPE PIC X(2) - level-88: BU/SL/TR/FE */
    @Column(name = "TRANSACTION_TYPE", length = 2, nullable = false)
    private String transactionType;

    /** TRN-QUANTITY PIC S9(11)V9(4) COMP-3 → BigDecimal scale 4 */
    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** TRN-PRICE PIC S9(11)V9(4) COMP-3 → BigDecimal scale 4 */
    @Column(name = "PRICE", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    /** TRN-AMOUNT PIC S9(13)V9(2) COMP-3 → BigDecimal scale 2 */
    @Column(name = "AMOUNT", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    /** TRN-CURRENCY PIC X(3) */
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    /** TRN-STATUS PIC X - level-88: P/D/F/R */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    /** Process timestamp */
    @Column(name = "PROCESS_DATE", nullable = false)
    private java.sql.Timestamp processDate;

    /** Process user */
    @Column(name = "PROCESS_USER", length = 8, nullable = false)
    private String processUser;

    public TransactionRecord() {
    }

    public TransactionRecord(TransactionRecord other) {
        if (other != null) {
            copyFrom(other);
        }
    }

    public void copyFrom(TransactionRecord other) {
        this.transactionId = other.transactionId;
        this.portfolioId = other.portfolioId;
        this.transactionDate = other.transactionDate;
        this.transactionTime = other.transactionTime;
        this.investmentId = other.investmentId;
        this.transactionType = other.transactionType;
        this.quantity = other.quantity;
        this.price = other.price;
        this.amount = other.amount;
        this.currencyCode = other.currencyCode;
        this.status = other.status;
        this.processDate = other.processDate;
        this.processUser = other.processUser;
    }

    // Getters and Setters

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

    public java.sql.Timestamp getProcessDate() {
        return processDate;
    }

    public void setProcessDate(java.sql.Timestamp processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }
}
