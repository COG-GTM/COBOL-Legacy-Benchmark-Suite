package com.cobolbenchmark.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Position History Record - migrated from DBTBLS.cpy POSHIST structure.
 * DB2 table for historical position data with COMP-3 decimal fields.
 */
@Entity
@Table(name = "POSHIST")
@IdClass(PoshistKey.class)
public class PoshistRecord {

    @Id
    @Column(name = "ACCOUNT_NO", length = 8, nullable = false)
    private String accountNo;

    @Id
    @Column(name = "PORTFOLIO_ID", length = 10, nullable = false)
    private String portfolioId;

    @Id
    @Column(name = "TRANS_DATE", nullable = false)
    private LocalDate transDate;

    @Id
    @Column(name = "TRANS_TIME", nullable = false)
    private LocalTime transTime;

    @Column(name = "TRANS_TYPE", length = 2, nullable = false)
    private String transType;

    @Column(name = "SECURITY_ID", length = 12, nullable = false)
    private String securityId;

    /** PH-QUANTITY COMP-3 → BigDecimal scale 3 */
    @Column(name = "QUANTITY", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    /** PH-PRICE COMP-3 → BigDecimal scale 3 */
    @Column(name = "PRICE", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    /** PH-AMOUNT COMP-3 → BigDecimal scale 2 */
    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** PH-FEES COMP-3 → BigDecimal scale 2 */
    @Column(name = "FEES", precision = 15, scale = 2, nullable = false)
    private BigDecimal fees = BigDecimal.ZERO;

    /** PH-TOTAL-AMOUNT COMP-3 → BigDecimal scale 2 */
    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** PH-COST-BASIS COMP-3 → BigDecimal scale 2 */
    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** PH-GAIN-LOSS COMP-3 → BigDecimal scale 2 */
    @Column(name = "GAIN_LOSS", precision = 15, scale = 2, nullable = false)
    private BigDecimal gainLoss;

    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDate processDate;

    @Column(name = "PROCESS_TIME", nullable = false)
    private LocalTime processTime;

    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    @Column(name = "AUDIT_TIMESTAMP")
    private java.sql.Timestamp auditTimestamp;

    public PoshistRecord() {
    }

    // Getters and Setters

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public LocalDate getTransDate() { return transDate; }
    public void setTransDate(LocalDate transDate) { this.transDate = transDate; }

    public LocalTime getTransTime() { return transTime; }
    public void setTransTime(LocalTime transTime) { this.transTime = transTime; }

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

    public LocalTime getProcessTime() { return processTime; }
    public void setProcessTime(LocalTime processTime) { this.processTime = processTime; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public java.sql.Timestamp getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(java.sql.Timestamp auditTimestamp) { this.auditTimestamp = auditTimestamp; }
}
