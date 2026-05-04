package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transaction entity - migrated from COBOL TRNREC.cpy.
 *
 * COBOL field mappings:
 * - TRN-QUANTITY (PIC S9(11)V9(4) COMP-3) -> quantity (BigDecimal, precision=15, scale=4)
 * - TRN-PRICE (PIC S9(11)V9(4) COMP-3) -> price (BigDecimal, precision=15, scale=4)
 * - TRN-AMOUNT (PIC S9(13)V9(2) COMP-3) -> amount (BigDecimal, precision=15, scale=2)
 * - TRN-TYPE level-88: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
 * - TRN-STATUS level-88: P=Pending, D=Done, F=Failed, R=Reversed
 */
@Entity
@Table(name = "transaction_history")
public class Transaction {

    @Id
    @Column(name = "transaction_id", length = 28, nullable = false)
    private String transactionId;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time", length = 6, nullable = false)
    private String transactionTime;

    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "transaction_type", length = 2, nullable = false)
    private String transactionType;

    @Column(name = "sequence_no", length = 6)
    private String sequenceNo;

    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "process_date", nullable = false)
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8, nullable = false)
    private String processUser;

    @Column(name = "filler", length = 50)
    private String filler;

    public Transaction() {
        this.quantity = BigDecimal.ZERO;
        this.price = BigDecimal.ZERO;
        this.amount = BigDecimal.ZERO;
        this.currencyCode = "USD";
        this.status = "P";
        this.processDate = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getTransactionTime() { return transactionTime; }
    public void setTransactionTime(String transactionTime) { this.transactionTime = transactionTime; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(String sequenceNo) { this.sequenceNo = sequenceNo; }
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
    public String getFiller() { return filler; }
    public void setFiller(String filler) { this.filler = filler; }

    public boolean isBuy() { return "BU".equals(transactionType); }
    public boolean isSell() { return "SL".equals(transactionType); }
    public boolean isTransfer() { return "TR".equals(transactionType); }
    public boolean isFee() { return "FE".equals(transactionType); }
    public boolean isPending() { return "P".equals(status); }
    public boolean isDone() { return "D".equals(status); }
    public boolean isFailed() { return "F".equals(status); }
    public boolean isReversed() { return "R".equals(status); }
}
