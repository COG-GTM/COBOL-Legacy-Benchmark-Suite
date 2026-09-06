package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DBTBLS.cpy POSHIST-RECORD / POSHIST.sql.
 * QUANTITY and PRICE are scale 3 (PH-QUANTITY / PH-PRICE PIC S9(12)V9(3)); the
 * remaining COMP-3 fields are scale 2. Values must come through ScaleConverter.
 */
@Entity
@Table(name = "POSHIST")
public class PositionHistory {

    @EmbeddedId
    private PositionHistoryId id;

    @Convert(converter = TransactionType.Converter.class)
    @Column(name = "TRANS_TYPE", length = 2, nullable = false)
    private TransactionType transType;

    @Column(name = "SECURITY_ID", length = 12, nullable = false)
    private String securityId;

    @Column(name = "QUANTITY", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "PRICE", precision = 15, scale = 3, nullable = false)
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
    private LocalDateTime auditTimestamp = LocalDateTime.now();

    protected PositionHistory() {
    }

    public PositionHistory(PositionHistoryId id, TransactionType transType, String securityId) {
        this.id = id;
        this.transType = transType;
        this.securityId = securityId;
    }

    public PositionHistoryId getId() {
        return id;
    }

    public TransactionType getTransType() {
        return transType;
    }

    public String getSecurityId() {
        return securityId;
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
