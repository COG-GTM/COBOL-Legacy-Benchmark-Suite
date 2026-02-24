package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Position History entity - migrated from DB2 POSHIST table.
 * Source: src/database/db2/POSHIST.sql, HISTREC copybook
 */
@Entity
@Table(name = "position_history")
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", length = 8, nullable = false)
    @NotBlank
    private String accountNo;

    @Column(name = "portfolio_id", length = 10, nullable = false)
    @NotBlank
    private String portfolioId;

    @Column(name = "trans_date", nullable = false)
    @NotNull
    private LocalDate transDate;

    @Column(name = "trans_time", nullable = false)
    @NotNull
    private LocalTime transTime;

    @Column(name = "trans_type", length = 2, nullable = false)
    @NotBlank
    private String transType;

    @Column(name = "security_id", length = 12, nullable = false)
    @NotBlank
    private String securityId;

    @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
    @NotNull
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "price", precision = 15, scale = 3, nullable = false)
    @NotNull
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "fees", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal fees = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal costBasis = BigDecimal.ZERO;

    @Column(name = "gain_loss", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal gainLoss = BigDecimal.ZERO;

    @Column(name = "process_date", nullable = false)
    @NotNull
    private LocalDate processDate;

    @Column(name = "process_time", nullable = false)
    @NotNull
    private LocalTime processTime;

    @Column(name = "program_id", length = 8, nullable = false)
    @NotBlank
    private String programId;

    @Column(name = "user_id", length = 8, nullable = false)
    @NotBlank
    private String userId;

    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp = LocalDateTime.now();

    public PositionHistory() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(LocalDateTime auditTimestamp) { this.auditTimestamp = auditTimestamp; }
}
