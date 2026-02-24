package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JPA Entity mapping for the Position History table (POSHIST).
 *
 * COBOL Source: HISTREC.cpy (HISTORY-RECORD)
 *   HIST-KEY: HIST-PORTFOLIO-ID + HIST-DATE + HIST-TIME + HIST-SEQ-NO
 *   HIST-DATA: HIST-RECORD-TYPE, HIST-ACTION-CODE, HIST-BEFORE-IMAGE,
 *              HIST-AFTER-IMAGE, HIST-REASON-CODE
 *   HIST-AUDIT: HIST-PROCESS-DATE, HIST-PROCESS-USER
 *
 * DB2 Source: POSHIST.sql
 *   PK: (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
 *   Partitioned by TRANS_DATE (quarterly)
 *
 * This entity represents the detailed position history with all transaction
 * fields from the POSHIST DB2 table, including fees, cost basis, and gain/loss.
 */
@Entity
@Table(name = "position_history")
public class TransactionHistory {

    @EmbeddedId
    private TransactionHistoryId id;

    /**
     * Transaction Type.
     * COBOL: HIST-RECORD-TYPE PIC X(02)
     * DB2: TRANS_TYPE CHAR(2) NOT NULL
     */
    @Column(name = "trans_type", length = 2, nullable = false)
    @NotNull
    @Size(max = 2)
    private String transType;

    /**
     * Security Identifier.
     * DB2: SECURITY_ID CHAR(12) NOT NULL
     */
    @Column(name = "security_id", length = 12, nullable = false)
    @NotNull
    @Size(max = 12)
    private String securityId;

    /**
     * Transaction Quantity.
     * COBOL: PH-QUANTITY PIC S9(12)V9(3) COMP-3
     * DB2: QUANTITY DECIMAL(15,3) NOT NULL
     */
    @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
    @NotNull
    private BigDecimal quantity;

    /**
     * Transaction Price.
     * COBOL: PH-PRICE PIC S9(12)V9(3) COMP-3
     * DB2: PRICE DECIMAL(15,3) NOT NULL
     */
    @Column(name = "price", precision = 15, scale = 3, nullable = false)
    @NotNull
    private BigDecimal price;

    /**
     * Transaction Amount.
     * COBOL: PH-AMOUNT PIC S9(13)V9(2) COMP-3
     * DB2: AMOUNT DECIMAL(15,2) NOT NULL
     */
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal amount;

    /**
     * Transaction Fees.
     * COBOL: PH-FEES PIC S9(13)V9(2) COMP-3
     * DB2: FEES DECIMAL(15,2) NOT NULL DEFAULT 0
     */
    @Column(name = "fees", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal fees = BigDecimal.ZERO;

    /**
     * Total Amount Including Fees.
     * COBOL: PH-TOTAL-AMOUNT PIC S9(13)V9(2) COMP-3
     * DB2: TOTAL_AMOUNT DECIMAL(15,2) NOT NULL
     */
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal totalAmount;

    /**
     * Cost Basis Amount.
     * COBOL: PH-COST-BASIS PIC S9(13)V9(2) COMP-3
     * DB2: COST_BASIS DECIMAL(15,2) NOT NULL
     */
    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal costBasis;

    /**
     * Realized Gain/Loss Amount.
     * COBOL: PH-GAIN-LOSS PIC S9(13)V9(2) COMP-3
     * DB2: GAIN_LOSS DECIMAL(15,2) NOT NULL
     */
    @Column(name = "gain_loss", precision = 15, scale = 2, nullable = false)
    @NotNull
    private BigDecimal gainLoss;

    /**
     * Processing Date.
     * COBOL: PH-PROCESS-DATE PIC X(10)
     * DB2: PROCESS_DATE DATE NOT NULL
     */
    @Column(name = "process_date", nullable = false)
    @NotNull
    private LocalDate processDate;

    /**
     * Processing Time.
     * COBOL: PH-PROCESS-TIME PIC X(8)
     * DB2: PROCESS_TIME TIME NOT NULL
     */
    @Column(name = "process_time", nullable = false)
    @NotNull
    private LocalTime processTime;

    /**
     * Program Identifier (audit field).
     * COBOL: PH-PROGRAM-ID PIC X(8)
     * DB2: PROGRAM_ID CHAR(8) NOT NULL
     */
    @Column(name = "program_id", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String programId;

    /**
     * User Identifier (audit field).
     * COBOL: PH-USER-ID PIC X(8)
     * DB2: USER_ID CHAR(8) NOT NULL
     */
    @Column(name = "user_id", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String userId;

    /**
     * Audit Timestamp.
     * COBOL: PH-AUDIT-TIMESTAMP PIC X(26)
     * DB2: AUDIT_TIMESTAMP TIMESTAMP NOT NULL WITH DEFAULT
     */
    @Column(name = "audit_timestamp", nullable = false)
    @NotNull
    private LocalDateTime auditTimestamp;

    public TransactionHistory() {
    }

    // --- Getters and Setters ---

    public TransactionHistoryId getId() {
        return id;
    }

    public void setId(TransactionHistoryId id) {
        this.id = id;
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
