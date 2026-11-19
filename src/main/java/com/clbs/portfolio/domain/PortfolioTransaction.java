package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Transaction record (copybook TRNREC, DB2 TRANSACTION_HISTORY). */
@Entity
@Table(name = "PORTFOLIO_TRANSACTION")
public class PortfolioTransaction {

    public static final String STATUS_PENDING = "P";
    public static final String STATUS_DONE = "D";
    public static final String STATUS_FAILED = "F";
    public static final String STATUS_REVERSED = "R";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TRANS_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "TRANS_TIME", nullable = false)
    private LocalTime transactionTime;

    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "INVESTMENT_ID", length = 10)
    private String investmentId;

    @Column(name = "TRANS_TYPE", length = 2, nullable = false)
    private String type;

    @Column(name = "QUANTITY", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "PRICE", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency;

    @Column(name = "STATUS", length = 1, nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "STATUS_MESSAGE", length = 80)
    private String statusMessage;

    @Column(name = "PROCESS_TS")
    private LocalDateTime processTimestamp;

    @Column(name = "PROCESS_USER", length = 8)
    private String processUser;

    protected PortfolioTransaction() {
    }

    public PortfolioTransaction(String portfolioId, String investmentId, String type, BigDecimal quantity,
                                BigDecimal price, BigDecimal amount, String currency, LocalDateTime now) {
        this.portfolioId = portfolioId;
        this.investmentId = investmentId;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.amount = amount;
        this.currency = currency;
        this.transactionDate = now.toLocalDate();
        this.transactionTime = now.toLocalTime().withNano(0);
    }

    public Long getId() { return id; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public LocalTime getTransactionTime() { return transactionTime; }
    public String getPortfolioId() { return portfolioId; }
    public String getInvestmentId() { return investmentId; }
    public String getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getStatusMessage() { return statusMessage; }
    public LocalDateTime getProcessTimestamp() { return processTimestamp; }
    public String getProcessUser() { return processUser; }

    public void complete(String status, String message, LocalDateTime processedAt, String user) {
        this.status = status;
        this.statusMessage = message;
        this.processTimestamp = processedAt;
        this.processUser = user;
    }
}
