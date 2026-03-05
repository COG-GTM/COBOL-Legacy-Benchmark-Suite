package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Transaction Record entity.
 * Migrated from COBOL TRNREC copybook.
 * PIC X -> String, PIC S9(n)V9(m) COMP-3 -> BigDecimal
 * Transaction types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
 * Status: P=Pending, D=Done, F=Failed, R=Reversed
 */
@Entity
@Table(name = "TRANSACTION_HISTORY")
public class TransactionRecord {

    @Id
    @Column(name = "TRANSACTION_ID", length = 20, nullable = false)
    private String transactionId;

    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "TRANSACTION_TIME", nullable = false)
    private LocalTime transactionTime;

    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    /** Transaction type: BU=Buy, SL=Sell, TR=Transfer, FE=Fee */
    @Column(name = "TRANSACTION_TYPE", length = 2, nullable = false)
    private String transactionType;

    /** PIC S9(11)V9(4) COMP-3 -> BigDecimal */
    @Column(name = "QUANTITY", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** PIC S9(11)V9(4) COMP-3 -> BigDecimal */
    @Column(name = "PRICE", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    /** PIC S9(13)V9(2) COMP-3 -> BigDecimal */
    @Column(name = "AMOUNT", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    /** Status: P=Pending, D=Done, F=Failed, R=Reversed */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDateTime processDate;

    @Column(name = "PROCESS_USER", length = 8, nullable = false)
    private String processUser;

    public TransactionRecord() {}

    // Getters and setters
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

    // COBOL transaction type constants
    public static final String TYPE_BUY = "BU";
    public static final String TYPE_SELL = "SL";
    public static final String TYPE_TRANSFER = "TR";
    public static final String TYPE_FEE = "FE";

    // COBOL status constants
    public static final String STATUS_PENDING = "P";
    public static final String STATUS_DONE = "D";
    public static final String STATUS_FAILED = "F";
    public static final String STATUS_REVERSED = "R";

    public boolean isBuy() { return TYPE_BUY.equals(transactionType); }
    public boolean isSell() { return TYPE_SELL.equals(transactionType); }
    public boolean isTransfer() { return TYPE_TRANSFER.equals(transactionType); }
    public boolean isFee() { return TYPE_FEE.equals(transactionType); }
}
