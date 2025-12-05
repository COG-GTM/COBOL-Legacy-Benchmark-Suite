package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Position History Entity
 * Migrated from: DB2 POSHIST with quarterly partitioning
 * COBOL Copybook: DBTBLS.cpy (POSHIST-RECORD)
 * 
 * Primary Key: ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME
 * Partitioned by: TRANS_DATE (quarterly)
 */
@Entity
@Table(name = "position_history",
        indexes = {
                @Index(name = "idx_poshist_account", columnList = "account_no, portfolio_id, trans_date"),
                @Index(name = "idx_poshist_security", columnList = "security_id, trans_date"),
                @Index(name = "idx_poshist_process", columnList = "process_date, program_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Account Number
     * COBOL: PH-ACCOUNT-NO PIC X(8)
     */
    @NotBlank(message = "Account number is required")
    @Size(max = 8, message = "Account number must not exceed 8 characters")
    @Column(name = "account_no", nullable = false, length = 8)
    private String accountNo;

    /**
     * Portfolio Identifier
     * COBOL: PH-PORTFOLIO-ID PIC X(10)
     */
    @NotBlank(message = "Portfolio ID is required")
    @Size(max = 10, message = "Portfolio ID must not exceed 10 characters")
    @Column(name = "portfolio_id", nullable = false, length = 10)
    private String portfolioId;

    /**
     * Transaction Date
     * COBOL: PH-TRANS-DATE PIC X(10)
     * Used for partitioning
     */
    @NotNull(message = "Transaction date is required")
    @Column(name = "trans_date", nullable = false)
    private LocalDate transDate;

    /**
     * Transaction Time
     * COBOL: PH-TRANS-TIME PIC X(8)
     */
    @NotNull(message = "Transaction time is required")
    @Column(name = "trans_time", nullable = false)
    private LocalTime transTime;

    /**
     * Transaction Type
     * COBOL: PH-TRANS-TYPE PIC X(2)
     * Values: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
     */
    @NotNull(message = "Transaction type is required")
    @Pattern(regexp = "BU|SL|TR|FE", message = "Transaction type must be BU, SL, TR, or FE")
    @Column(name = "trans_type", nullable = false, length = 2)
    private String transType;

    /**
     * Security Identifier
     * COBOL: PH-SECURITY-ID PIC X(12)
     */
    @NotBlank(message = "Security ID is required")
    @Size(max = 12, message = "Security ID must not exceed 12 characters")
    @Column(name = "security_id", nullable = false, length = 12)
    private String securityId;

    /**
     * Transaction Quantity
     * COBOL: PH-QUANTITY PIC S9(12)V9(3) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "-999999999999.999", message = "Quantity is below minimum")
    @DecimalMax(value = "999999999999.999", message = "Quantity exceeds maximum")
    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * Transaction Price
     * COBOL: PH-PRICE PIC S9(12)V9(3) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "-999999999999.999", message = "Price is below minimum")
    @DecimalMax(value = "999999999999.999", message = "Price exceeds maximum")
    @Column(name = "price", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    /**
     * Transaction Amount
     * COBOL: PH-AMOUNT PIC S9(13)V9(2) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "-9999999999999.99", message = "Amount is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Amount exceeds maximum")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * Transaction Fees
     * COBOL: PH-FEES PIC S9(13)V9(2) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Fees is required")
    @DecimalMin(value = "-9999999999999.99", message = "Fees is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Fees exceeds maximum")
    @Column(name = "fees", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;

    /**
     * Total Amount Including Fees
     * COBOL: PH-TOTAL-AMOUNT PIC S9(13)V9(2) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "-9999999999999.99", message = "Total amount is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Total amount exceeds maximum")
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Cost Basis Amount
     * COBOL: PH-COST-BASIS PIC S9(13)V9(2) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Cost basis is required")
    @DecimalMin(value = "-9999999999999.99", message = "Cost basis is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Cost basis exceeds maximum")
    @Column(name = "cost_basis", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal costBasis = BigDecimal.ZERO;

    /**
     * Realized Gain/Loss Amount
     * COBOL: PH-GAIN-LOSS PIC S9(13)V9(2) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Gain/loss is required")
    @DecimalMin(value = "-9999999999999.99", message = "Gain/loss is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Gain/loss exceeds maximum")
    @Column(name = "gain_loss", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal gainLoss = BigDecimal.ZERO;

    /**
     * Process Date
     * COBOL: PH-PROCESS-DATE PIC X(10)
     */
    @NotNull(message = "Process date is required")
    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    /**
     * Process Time
     * COBOL: PH-PROCESS-TIME PIC X(8)
     */
    @NotNull(message = "Process time is required")
    @Column(name = "process_time", nullable = false)
    private LocalTime processTime;

    /**
     * Program Identifier
     * COBOL: PH-PROGRAM-ID PIC X(8)
     */
    @NotBlank(message = "Program ID is required")
    @Size(max = 8, message = "Program ID must not exceed 8 characters")
    @Column(name = "program_id", nullable = false, length = 8)
    private String programId;

    /**
     * User Identifier
     * COBOL: PH-USER-ID PIC X(8)
     */
    @NotBlank(message = "User ID is required")
    @Size(max = 8, message = "User ID must not exceed 8 characters")
    @Column(name = "user_id", nullable = false, length = 8)
    private String userId;

    /**
     * Audit Timestamp
     * COBOL: PH-AUDIT-TIMESTAMP PIC X(26)
     */
    @NotNull(message = "Audit timestamp is required")
    @Column(name = "audit_timestamp", nullable = false)
    @Builder.Default
    private OffsetDateTime auditTimestamp = OffsetDateTime.now();

    /**
     * Check if transaction is a buy
     */
    public boolean isBuy() {
        return "BU".equals(this.transType);
    }

    /**
     * Check if transaction is a sell
     */
    public boolean isSell() {
        return "SL".equals(this.transType);
    }

    /**
     * Check if transaction is a transfer
     */
    public boolean isTransfer() {
        return "TR".equals(this.transType);
    }

    /**
     * Check if transaction is a fee
     */
    public boolean isFee() {
        return "FE".equals(this.transType);
    }

    /**
     * Check if there is a realized gain
     */
    public boolean hasGain() {
        return this.gainLoss.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if there is a realized loss
     */
    public boolean hasLoss() {
        return this.gainLoss.compareTo(BigDecimal.ZERO) < 0;
    }
}
