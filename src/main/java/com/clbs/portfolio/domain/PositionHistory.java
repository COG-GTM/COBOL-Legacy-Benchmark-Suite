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

/** Position history row (DB2 table POSHIST, loaded by HISTLD00, read by INQHIST). */
@Entity
@Table(name = "POSITION_HISTORY")
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ACCOUNT_NO", length = 10, nullable = false)
    private String accountNo;

    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "TRANS_DATE", nullable = false)
    private LocalDate transDate;

    @Column(name = "TRANS_TIME", nullable = false)
    private LocalTime transTime;

    @Column(name = "TRANS_TYPE", length = 2, nullable = false)
    private String transType;

    @Column(name = "SECURITY_ID", length = 12, nullable = false)
    private String securityId;

    @Column(name = "QUANTITY", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "PRICE", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "FEES", precision = 15, scale = 2, nullable = false)
    private BigDecimal fees = BigDecimal.ZERO.setScale(2);

    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "GAIN_LOSS", precision = 15, scale = 2, nullable = false)
    private BigDecimal gainLoss = BigDecimal.ZERO.setScale(2);

    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    @Column(name = "AUDIT_TS", nullable = false)
    private LocalDateTime auditTimestamp;

    protected PositionHistory() {
    }

    public PositionHistory(String accountNo, String portfolioId, LocalDate transDate, LocalTime transTime,
                           String transType, String securityId, BigDecimal quantity, BigDecimal price,
                           BigDecimal amount, BigDecimal costBasis, String programId, String userId,
                           LocalDateTime auditTimestamp) {
        this.accountNo = accountNo;
        this.portfolioId = portfolioId;
        this.transDate = transDate;
        this.transTime = transTime;
        this.transType = transType;
        this.securityId = securityId;
        this.quantity = quantity;
        this.price = price;
        this.amount = amount;
        this.totalAmount = amount;
        this.costBasis = costBasis;
        this.programId = programId;
        this.userId = userId;
        this.auditTimestamp = auditTimestamp;
    }

    public Long getId() { return id; }
    public String getAccountNo() { return accountNo; }
    public String getPortfolioId() { return portfolioId; }
    public LocalDate getTransDate() { return transDate; }
    public LocalTime getTransTime() { return transTime; }
    public String getTransType() { return transType; }
    public String getSecurityId() { return securityId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getFees() { return fees; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getCostBasis() { return costBasis; }
    public BigDecimal getGainLoss() { return gainLoss; }
    public String getProgramId() { return programId; }
    public String getUserId() { return userId; }
    public LocalDateTime getAuditTimestamp() { return auditTimestamp; }
}
