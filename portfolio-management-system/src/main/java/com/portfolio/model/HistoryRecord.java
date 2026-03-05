package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * History Record entity.
 * Migrated from COBOL HISTREC copybook.
 * Record types: PT=Portfolio, PS=Position, TR=Transaction
 * Action codes: A=Add, C=Change, D=Delete
 */
@Entity
@Table(name = "TRANSACTION_HISTORY_VSAM")
@IdClass(HistoryRecordKey.class)
public class HistoryRecord {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "TXN_DATE", nullable = false)
    private java.time.LocalDate txnDate;

    @Id
    @Column(name = "SEQ", nullable = false)
    private int seq;

    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "TRANSACTION_TYPE", length = 2, nullable = false)
    private String transactionType;

    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private java.math.BigDecimal quantity;

    @Column(name = "PRICE", precision = 18, scale = 4, nullable = false)
    private java.math.BigDecimal price;

    @Column(name = "AMOUNT", precision = 18, scale = 2, nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    @Column(name = "COST_BASIS", precision = 18, scale = 2, nullable = false)
    private java.math.BigDecimal costBasis;

    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDateTime processDate;

    @Column(name = "PROCESS_USER", length = 8, nullable = false)
    private String processUser;

    // Record type constants from HISTREC copybook
    public static final String TYPE_PORTFOLIO = "PT";
    public static final String TYPE_POSITION = "PS";
    public static final String TYPE_TRANSACTION = "TR";

    // Action code constants
    public static final String ACTION_ADD = "A";
    public static final String ACTION_CHANGE = "C";
    public static final String ACTION_DELETE = "D";

    public HistoryRecord() {}

    // Getters and setters
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public java.time.LocalDate getTxnDate() { return txnDate; }
    public void setTxnDate(java.time.LocalDate txnDate) { this.txnDate = txnDate; }

    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }

    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public java.math.BigDecimal getQuantity() { return quantity; }
    public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }

    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }

    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.math.BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(java.math.BigDecimal costBasis) { this.costBasis = costBasis; }

    public LocalDateTime getProcessDate() { return processDate; }
    public void setProcessDate(LocalDateTime processDate) { this.processDate = processDate; }

    public String getProcessUser() { return processUser; }
    public void setProcessUser(String processUser) { this.processUser = processUser; }
}
