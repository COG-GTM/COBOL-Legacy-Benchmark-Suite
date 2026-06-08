package com.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * JPA entity mapping the POSHIST-RECORD from DB2 DBTBLS.cpy.
 */
@Entity
@Table(name = "position_history")
public class PositionHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", length = 8)
    private String accountNo;

    @Column(name = "portfolio_id", length = 10)
    private String portfolioId;

    @Column(name = "trans_date", length = 10)
    private String transDate;

    @Column(name = "trans_time", length = 8)
    private String transTime;

    @Column(name = "trans_type", length = 2)
    private String transType;

    @Column(name = "security_id", length = 12)
    private String securityId;

    @Column(name = "quantity", precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 3)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fees", precision = 15, scale = 2)
    private BigDecimal fees;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "cost_basis", precision = 15, scale = 2)
    private BigDecimal costBasis;

    @Column(name = "gain_loss", precision = 15, scale = 2)
    private BigDecimal gainLoss;

    @Column(name = "process_date", length = 10)
    private String processDate;

    @Column(name = "process_time", length = 8)
    private String processTime;

    @Column(name = "program_id", length = 8)
    private String programId;

    @Column(name = "user_id", length = 8)
    private String userId;

    @Column(name = "audit_timestamp", length = 26)
    private String auditTimestamp;

    protected PositionHistoryEntity() { /* JPA */ }

    public Long getId() { return id; }
    public String getAccountNo() { return accountNo; }
    public String getPortfolioId() { return portfolioId; }
    public String getTransDate() { return transDate; }
    public String getTransTime() { return transTime; }
    public String getTransType() { return transType; }
    public String getSecurityId() { return securityId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getFees() { return fees; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getCostBasis() { return costBasis; }
    public BigDecimal getGainLoss() { return gainLoss; }
    public String getProcessDate() { return processDate; }
    public String getProcessTime() { return processTime; }
    public String getProgramId() { return programId; }
    public String getUserId() { return userId; }
    public String getAuditTimestamp() { return auditTimestamp; }

    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public void setTransDate(String transDate) { this.transDate = transDate; }
    public void setTransTime(String transTime) { this.transTime = transTime; }
    public void setTransType(String transType) { this.transType = transType; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }
    public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
    public void setProcessDate(String processDate) { this.processDate = processDate; }
    public void setProcessTime(String processTime) { this.processTime = processTime; }
    public void setProgramId(String programId) { this.programId = programId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setAuditTimestamp(String auditTimestamp) { this.auditTimestamp = auditTimestamp; }
}
