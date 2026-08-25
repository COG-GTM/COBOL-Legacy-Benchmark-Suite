package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JPA entity for the DB2 TRANSACTION_HISTORY table
 * ({@code src/database/db2/db2-definitions.sql}); the corresponding COBOL
 * record is 01 TRANSACTION-RECORD in {@code src/copybook/common/TRNREC.cpy}.
 *
 * <p>TRANSACTION_ID format: YYYYMMDDHHMMSS + 6-digit sequence.
 */
@Entity
@Table(name = "TRANSACTION_HISTORY")
public class TransactionHistory {

    /** TRANSACTION_ID CHAR(20). */
    @Id
    @Column(name = "TRANSACTION_ID", length = 20, nullable = false)
    private String transactionId;

    /** PORTFOLIO_ID CHAR(8) / TRN-PORTFOLIO-ID PIC X(08). */
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    /** TRANSACTION_DATE DATE / TRN-DATE PIC X(08). */
    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    /** TRANSACTION_TIME TIME / TRN-TIME PIC X(06). */
    @Column(name = "TRANSACTION_TIME", nullable = false)
    private LocalTime transactionTime;

    /** INVESTMENT_ID CHAR(10) / TRN-INVESTMENT-ID PIC X(10). */
    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    /** TRANSACTION_TYPE CHAR(2) / TRN-TYPE PIC X(02) — BU/SL/TR/FE. */
    @Column(name = "TRANSACTION_TYPE", length = 2, nullable = false)
    private String transactionType;

    /** QUANTITY DECIMAL(18,4) / TRN-QUANTITY PIC S9(11)V9(4) COMP-3. */
    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** PRICE DECIMAL(18,4) / TRN-PRICE PIC S9(11)V9(4) COMP-3. */
    @Column(name = "PRICE", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    /** AMOUNT DECIMAL(18,2) / TRN-AMOUNT PIC S9(13)V9(2) COMP-3. */
    @Column(name = "AMOUNT", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    /** CURRENCY_CODE CHAR(3) / TRN-CURRENCY PIC X(03). */
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    /** STATUS CHAR(1) / TRN-STATUS PIC X(01) — P/F/R (per DDL notes). */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    /** PROCESS_DATE TIMESTAMP / TRN-PROCESS-DATE PIC X(26). */
    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDateTime processDate;

    /** PROCESS_USER VARCHAR(8) / TRN-PROCESS-USER PIC X(08). */
    @Column(name = "PROCESS_USER", length = 8, nullable = false)
    private String processUser;

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
}
