package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * JPA entity for the DB2 POSHIST table
 * ({@code src/database/db2/POSHIST.sql}); the corresponding COBOL host
 * structure is 01 POSHIST-RECORD in {@code src/copybook/db2/DBTBLS.cpy}.
 *
 * <p>Primary key: (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME).
 * Packed-decimal columns (COMP-3 / DECIMAL) map to {@link BigDecimal}.
 */
@Entity
@Table(name = "POSHIST")
public class PositionHistory {

    @EmbeddedId
    private Key key;

    /** PH-TRANS-TYPE PIC X(2) / TRANS_TYPE CHAR(2) — BU/SL/TR/FE. */
    @Column(name = "TRANS_TYPE", length = 2, nullable = false)
    private String transType;

    /** PH-SECURITY-ID PIC X(12) / SECURITY_ID CHAR(12). */
    @Column(name = "SECURITY_ID", length = 12, nullable = false)
    private String securityId;

    /** PH-QUANTITY PIC S9(12)V9(3) COMP-3 / QUANTITY DECIMAL(15,3). */
    @Column(name = "QUANTITY", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    /** PH-PRICE PIC S9(12)V9(3) COMP-3 / PRICE DECIMAL(15,3). */
    @Column(name = "PRICE", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    /** PH-AMOUNT PIC S9(13)V9(2) COMP-3 / AMOUNT DECIMAL(15,2). */
    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** PH-FEES PIC S9(13)V9(2) COMP-3 / FEES DECIMAL(15,2) DEFAULT 0. */
    @Column(name = "FEES", precision = 15, scale = 2, nullable = false)
    private BigDecimal fees = BigDecimal.ZERO;

    /** PH-TOTAL-AMOUNT PIC S9(13)V9(2) COMP-3 / TOTAL_AMOUNT DECIMAL(15,2). */
    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** PH-COST-BASIS PIC S9(13)V9(2) COMP-3 / COST_BASIS DECIMAL(15,2). */
    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** PH-GAIN-LOSS PIC S9(13)V9(2) COMP-3 / GAIN_LOSS DECIMAL(15,2). */
    @Column(name = "GAIN_LOSS", precision = 15, scale = 2, nullable = false)
    private BigDecimal gainLoss;

    /** PH-PROCESS-DATE PIC X(10) / PROCESS_DATE DATE. */
    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDate processDate;

    /** PH-PROCESS-TIME PIC X(8) / PROCESS_TIME TIME. */
    @Column(name = "PROCESS_TIME", nullable = false)
    private LocalTime processTime;

    /** PH-PROGRAM-ID PIC X(8) / PROGRAM_ID CHAR(8). */
    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    /** PH-USER-ID PIC X(8) / USER_ID CHAR(8). */
    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    /** PH-AUDIT-TIMESTAMP PIC X(26) / AUDIT_TIMESTAMP TIMESTAMP. */
    @Column(name = "AUDIT_TIMESTAMP", nullable = false)
    private LocalDateTime auditTimestamp;

    /** Composite primary key (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME). */
    @Embeddable
    public static class Key implements Serializable {

        /** PH-ACCOUNT-NO PIC X(8) / ACCOUNT_NO CHAR(8). */
        @Column(name = "ACCOUNT_NO", length = 8, nullable = false)
        private String accountNo;

        /** PH-PORTFOLIO-ID PIC X(10) / PORTFOLIO_ID CHAR(10). */
        @Column(name = "PORTFOLIO_ID", length = 10, nullable = false)
        private String portfolioId;

        /** PH-TRANS-DATE PIC X(10) / TRANS_DATE DATE. */
        @Column(name = "TRANS_DATE", nullable = false)
        private LocalDate transDate;

        /** PH-TRANS-TIME PIC X(8) / TRANS_TIME TIME. */
        @Column(name = "TRANS_TIME", nullable = false)
        private LocalTime transTime;

        public Key() {}

        public Key(String accountNo, String portfolioId, LocalDate transDate, LocalTime transTime) {
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
            if (!(o instanceof Key key)) return false;
            return Objects.equals(accountNo, key.accountNo)
                    && Objects.equals(portfolioId, key.portfolioId)
                    && Objects.equals(transDate, key.transDate)
                    && Objects.equals(transTime, key.transTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accountNo, portfolioId, transDate, transTime);
        }
    }

    public Key getKey() { return key; }
    public void setKey(Key key) { this.key = key; }
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
