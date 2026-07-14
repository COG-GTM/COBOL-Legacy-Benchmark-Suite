package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "poshist")
@IdClass(Poshist.PoshistId.class)
public class Poshist {

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
    @Column(name = "trans_time", nullable = false)
    private LocalTime transTime;

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

    @Column(name = "process_time", nullable = false)
    private LocalTime processTime;

    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;

    public Poshist() {
    }

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

    public static class PoshistId implements Serializable {
        private String accountNo;
        private String portfolioId;
        private LocalDate transDate;
        private LocalTime transTime;

        public PoshistId() {
        }

        public PoshistId(String accountNo, String portfolioId, LocalDate transDate, LocalTime transTime) {
            this.accountNo = accountNo;
            this.portfolioId = portfolioId;
            this.transDate = transDate;
            this.transTime = transTime;
        }

        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

        public String getPortfolioId() { return portfolioId; }
        public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

        public LocalDate getTransDate() { return transDate; }
        public void setTransDate(LocalDate transDate) { this.transDate = transDate; }

        public LocalTime getTransTime() { return transTime; }
        public void setTransTime(LocalTime transTime) { this.transTime = transTime; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PoshistId)) return false;
            PoshistId that = (PoshistId) o;
            return Objects.equals(accountNo, that.accountNo)
                    && Objects.equals(portfolioId, that.portfolioId)
                    && Objects.equals(transDate, that.transDate)
                    && Objects.equals(transTime, that.transTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountNo, portfolioId, transDate, transTime);
        }
    }
}
