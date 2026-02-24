package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JPA Entity mapping for the Transaction History table.
 *
 * COBOL Source: TRNREC.cpy (TRANSACTION-RECORD)
 *   TRN-KEY: TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO
 *   TRN-DATA: TRN-INVESTMENT-ID, TRN-TYPE, TRN-QUANTITY, TRN-PRICE,
 *             TRN-AMOUNT, TRN-CURRENCY, TRN-STATUS
 *   TRN-AUDIT: TRN-PROCESS-DATE, TRN-PROCESS-USER
 *
 * DB2 Source: db2-definitions.sql (TRANSACTION_HISTORY)
 *   PK: TRANSACTION_ID
 *   FK: PORTFOLIO_ID -> PORTFOLIO_MASTER(PORTFOLIO_ID)
 *
 * Transaction types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
 * Status codes: P=Processed, F=Failed, R=Reversed
 * Transaction ID format: YYYYMMDDHHMMSS + 6-digit sequence
 */
@Entity
@Table(name = "transaction_history")
public class Transaction {

    /**
     * Transaction Identifier.
     * COBOL: TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO
     * DB2: TRANSACTION_ID CHAR(20) NOT NULL
     * Format: YYYYMMDDHHMMSS + 6-digit sequence
     */
    @Id
    @Column(name = "transaction_id", length = 20, nullable = false)
    @NotNull
    @Size(max = 20)
    private String transactionId;

    /**
     * Portfolio Identifier (FK to PORTFOLIO_MASTER).
     * COBOL: TRN-PORTFOLIO-ID PIC X(08)
     * DB2: PORTFOLIO_ID CHAR(8) NOT NULL
     */
    @Column(name = "portfolio_id", length = 8, nullable = false, insertable = false, updatable = false)
    private String portfolioId;

    /**
     * Transaction Date.
     * COBOL: TRN-DATE PIC X(08) (YYYYMMDD)
     * DB2: TRANSACTION_DATE DATE NOT NULL
     */
    @Column(name = "transaction_date", nullable = false)
    @NotNull
    private LocalDate transactionDate;

    /**
     * Transaction Time.
     * COBOL: TRN-TIME PIC X(06) (HHMMSS)
     * DB2: TRANSACTION_TIME TIME NOT NULL
     */
    @Column(name = "transaction_time", nullable = false)
    @NotNull
    private LocalTime transactionTime;

    /**
     * Investment Identifier.
     * COBOL: TRN-INVESTMENT-ID PIC X(10)
     * DB2: INVESTMENT_ID CHAR(10) NOT NULL
     */
    @Column(name = "investment_id", length = 10, nullable = false)
    @NotNull
    @Size(max = 10)
    private String investmentId;

    /**
     * Transaction Type.
     * COBOL: TRN-TYPE PIC X(02) — BU=Buy, SL=Sell, TR=Transfer, FE=Fee
     * DB2: TRANSACTION_TYPE CHAR(2) NOT NULL
     */
    @Column(name = "transaction_type", length = 2, nullable = false)
    @NotNull
    @Size(max = 2)
    private String transactionType;

    /**
     * Transaction Quantity.
     * COBOL: TRN-QUANTITY PIC S9(11)V9(4) COMP-3
     * DB2: QUANTITY DECIMAL(18,4) NOT NULL
     */
    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    @NotNull
    private BigDecimal quantity;

    /**
     * Transaction Price per unit.
     * COBOL: TRN-PRICE PIC S9(11)V9(4) COMP-3
     * DB2: PRICE DECIMAL(18,4) NOT NULL
     */
    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    @NotNull
    private BigDecimal price;

    /**
     * Transaction Amount.
     * COBOL: TRN-AMOUNT PIC S9(13)V9(2) COMP-3
     * DB2: AMOUNT DECIMAL(18,2) NOT NULL
     */
    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    @NotNull
    private BigDecimal amount;

    /**
     * Currency Code (e.g. USD, EUR, GBP).
     * COBOL: TRN-CURRENCY PIC X(03)
     * DB2: CURRENCY_CODE CHAR(3) NOT NULL
     */
    @Column(name = "currency_code", length = 3, nullable = false)
    @NotNull
    @Size(max = 3)
    private String currencyCode;

    /**
     * Transaction Status.
     * COBOL: TRN-STATUS PIC X(01) — P=Pending/Processed, F=Failed, R=Reversed
     * DB2: STATUS CHAR(1) NOT NULL
     */
    @Column(name = "status", length = 1, nullable = false)
    @NotNull
    @Size(max = 1)
    private String status;

    /**
     * Processing Timestamp (audit field).
     * COBOL: TRN-PROCESS-DATE PIC X(26)
     * DB2: PROCESS_DATE TIMESTAMP NOT NULL
     */
    @Column(name = "process_date", nullable = false)
    @NotNull
    private LocalDateTime processDate;

    /**
     * Processing User (audit field).
     * COBOL: TRN-PROCESS-USER PIC X(08)
     * DB2: PROCESS_USER VARCHAR(8) NOT NULL
     */
    @Column(name = "process_user", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String processUser;

    /**
     * Many-to-one relationship to Portfolio Master.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioMaster portfolio;

    public Transaction() {
    }

    // --- Getters and Setters ---

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPortfolioId() {
        return portfolioId;
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
}
