package com.portfolio.portmstr.model;

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

/**
 * Position History entity.
 * Mapped from COBOL copybook DBTBLS.cpy (POSHIST-RECORD) and
 * DB2 table POSHIST (POSHIST.sql).
 */
@Entity
@Table(name = "POSITION_HISTORY")
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ACCOUNT_NO", length = 8, nullable = false)
    private String accountNo;

    @Column(name = "PORTFOLIO_ID", length = 10, nullable = false)
    private String portfolioId;

    @Column(name = "TRANS_DATE", nullable = false)
    private LocalDate transDate;

    @Column(name = "TRANS_TIME", nullable = false)
    private LocalTime transTime;

    @Column(name = "TRANS_TYPE", length = 2, nullable = false)
    private String transType;

    @Column(name = "SECURITY_ID", length = 12, nullable = false)
    private String securityId;

    @Column(name = "QUANTITY", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "PRICE", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "FEES", precision = 15, scale = 2)
    private BigDecimal fees;

    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

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

    @Column(name = "AUDIT_TIMESTAMP", nullable = false)
    private LocalDateTime auditTimestamp;

    public PositionHistory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getTransDate() {
        return transDate;
    }

    public void setTransDate(LocalDate transDate) {
        this.transDate = transDate;
    }

    public LocalTime getTransTime() {
        return transTime;
    }

    public void setTransTime(LocalTime transTime) {
        this.transTime = transTime;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public String getSecurityId() {
        return securityId;
    }

    public void setSecurityId(String securityId) {
        this.securityId = securityId;
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

    public BigDecimal getFees() {
        return fees;
    }

    public void setFees(BigDecimal fees) {
        this.fees = fees;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public BigDecimal getGainLoss() {
        return gainLoss;
    }

    public void setGainLoss(BigDecimal gainLoss) {
        this.gainLoss = gainLoss;
    }

    public LocalDate getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDate processDate) {
        this.processDate = processDate;
    }

    public LocalTime getProcessTime() {
        return processTime;
    }

    public void setProcessTime(LocalTime processTime) {
        this.processTime = processTime;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getAuditTimestamp() {
        return auditTimestamp;
    }

    public void setAuditTimestamp(LocalDateTime auditTimestamp) {
        this.auditTimestamp = auditTimestamp;
    }
}
