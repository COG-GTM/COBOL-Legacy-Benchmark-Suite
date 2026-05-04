package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Position History Record entity - migrated from COBOL DBTBLS.cpy (POSHIST-RECORD).
 *
 * COBOL COMP-3 field mappings:
 * - PH-QUANTITY (PIC S9(12)V9(3) COMP-3) -> quantity (BigDecimal, precision=15, scale=3)
 * - PH-PRICE (PIC S9(12)V9(3) COMP-3) -> price (BigDecimal, precision=15, scale=3)
 * - PH-AMOUNT/FEES/TOTAL/COST/GAIN (PIC S9(13)V9(2) COMP-3) -> BigDecimal(15,2)
 */
@Entity
@Table(name = "poshist")
@IdClass(PosHistId.class)
public class PosHistRecord {

    @Id
    @Column(name = "account_no", length = 8, nullable = false)
    private String accountNo;

    @Id
    @Column(name = "portfolio_id", length = 10, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "trans_date", nullable = false)
    private LocalDate transDate;

    @Id
    @Column(name = "trans_time", length = 8, nullable = false)
    private String transTime;

    @Column(name = "trans_type", length = 2, nullable = false)
    private String transType;

    @Column(name = "security_id", length = 12, nullable = false)
    private String securityId;

    @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "fees", precision = 15, scale = 2, nullable = false)
    private BigDecimal fees;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @Column(name = "gain_loss", precision = 15, scale = 2, nullable = false)
    private BigDecimal gainLoss;

    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    @Column(name = "process_time", length = 8, nullable = false)
    private String processTime;

    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;

    public PosHistRecord() {
        this.quantity = BigDecimal.ZERO;
        this.price = BigDecimal.ZERO;
        this.amount = BigDecimal.ZERO;
        this.fees = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.costBasis = BigDecimal.ZERO;
        this.gainLoss = BigDecimal.ZERO;
        this.auditTimestamp = LocalDateTime.now();
    }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public LocalDate getTransDate() { return transDate; }
    public void setTransDate(LocalDate transDate) { this.transDate = transDate; }
    public String getTransTime() { return transTime; }
    public void setTransTime(String transTime) { this.transTime = transTime; }
    public String getTransType() { return transType; }
    public void setTransType(String transType) { this.transType = transType; }
    public String getSecurityId() { return securityId; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    public BigDecimal getGainLoss() { return gainLoss; }
    public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
    public LocalDate getProcessDate() { return processDate; }
    public void setProcessDate(LocalDate processDate) { this.processDate = processDate; }
    public String getProcessTime() { return processTime; }
    public void setProcessTime(String processTime) { this.processTime = processTime; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(LocalDateTime auditTimestamp) { this.auditTimestamp = auditTimestamp; }
}
