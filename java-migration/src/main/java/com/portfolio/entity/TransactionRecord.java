package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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

/**
 * Transaction Record Entity
 * Migrated from: VSAM TRANHIST (KSDS, 300 bytes)
 * COBOL Copybook: TRNREC.cpy
 * 
 * Key Structure:
 * - Transaction Date (8 bytes, YYYYMMDD)
 * - Transaction Time (6 bytes, HHMMSS)
 * - Portfolio ID (8 bytes)
 * - Sequence Number (6 bytes)
 */
@Entity
@Table(name = "transaction_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transaction_records_key",
                        columnNames = {"transaction_date", "transaction_time", "portfolio_id", "sequence_no"})
        },
        indexes = {
                @Index(name = "idx_transactions_portfolio", columnList = "portfolio_id, transaction_date"),
                @Index(name = "idx_transactions_date", columnList = "transaction_date, portfolio_id"),
                @Index(name = "idx_transactions_status", columnList = "status, transaction_date"),
                @Index(name = "idx_transactions_investment", columnList = "investment_id, transaction_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Transaction Identifier
     * Format: YYYYMMDDHHMMSS + 6-digit sequence
     */
    @NotBlank(message = "Transaction ID is required")
    @Size(max = 20, message = "Transaction ID must not exceed 20 characters")
    @Column(name = "transaction_id", nullable = false, length = 20)
    private String transactionId;

    /**
     * Portfolio Identifier
     * COBOL: TRN-PORTFOLIO-ID PIC X(08)
     */
    @NotBlank(message = "Portfolio ID is required")
    @Size(max = 8, message = "Portfolio ID must not exceed 8 characters")
    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    /**
     * Transaction Date
     * COBOL: TRN-DATE PIC X(08) (YYYYMMDD)
     */
    @NotNull(message = "Transaction date is required")
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /**
     * Transaction Time
     * COBOL: TRN-TIME PIC X(06) (HHMMSS)
     */
    @NotNull(message = "Transaction time is required")
    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    /**
     * Sequence Number
     * COBOL: TRN-SEQUENCE-NO PIC X(06)
     */
    @NotBlank(message = "Sequence number is required")
    @Size(max = 6, message = "Sequence number must not exceed 6 characters")
    @Column(name = "sequence_no", nullable = false, length = 6)
    private String sequenceNo;

    /**
     * Investment Identifier
     * COBOL: TRN-INVESTMENT-ID PIC X(10)
     */
    @NotBlank(message = "Investment ID is required")
    @Size(max = 10, message = "Investment ID must not exceed 10 characters")
    @Column(name = "investment_id", nullable = false, length = 10)
    private String investmentId;

    /**
     * Transaction Type
     * COBOL: TRN-TYPE PIC X(02)
     * Values: BU=Buy, SL=Sell, TR=Transfer, FE=Fee
     */
    @NotNull(message = "Transaction type is required")
    @Pattern(regexp = "BU|SL|TR|FE", message = "Transaction type must be BU (Buy), SL (Sell), TR (Transfer), or FE (Fee)")
    @Column(name = "transaction_type", nullable = false, length = 2)
    private String transactionType;

    /**
     * Transaction Quantity
     * COBOL: TRN-QUANTITY PIC S9(11)V9(4) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "-99999999999.9999", message = "Quantity is below minimum")
    @DecimalMax(value = "99999999999.9999", message = "Quantity exceeds maximum")
    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * Transaction Price
     * COBOL: TRN-PRICE PIC S9(11)V9(4) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "-99999999999.9999", message = "Price is below minimum")
    @DecimalMax(value = "99999999999.9999", message = "Price exceeds maximum")
    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    /**
     * Transaction Amount
     * COBOL: TRN-AMOUNT PIC S9(13)V9(2) COMP-3
     * Using BigDecimal for financial precision
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "-9999999999999.99", message = "Amount is below minimum")
    @DecimalMax(value = "9999999999999.99", message = "Amount exceeds maximum")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * Currency Code
     * COBOL: TRN-CURRENCY PIC X(03)
     */
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    /**
     * Transaction Status
     * COBOL: TRN-STATUS PIC X(01)
     * Values: P=Pending, D=Done, F=Failed, R=Reversed
     */
    @NotNull(message = "Status is required")
    @Pattern(regexp = "[PDFR]", message = "Status must be P (Pending), D (Done), F (Failed), or R (Reversed)")
    @Column(name = "status", nullable = false, length = 1)
    @Builder.Default
    private String status = "P";

    /**
     * Balance Before Transaction
     * COBOL: HIST-BEFORE-BAL PIC S9(11)V999
     */
    @Column(name = "before_balance", precision = 15, scale = 4)
    private BigDecimal beforeBalance;

    /**
     * Balance After Transaction
     * COBOL: HIST-AFTER-BAL PIC S9(11)V999
     */
    @Column(name = "after_balance", precision = 15, scale = 4)
    private BigDecimal afterBalance;

    /**
     * Result Code
     * COBOL: HIST-RESULT-CODE PIC X(04)
     */
    @Size(max = 4, message = "Result code must not exceed 4 characters")
    @Column(name = "result_code", length = 4)
    private String resultCode;

    /**
     * Check if transaction is a buy
     */
    public boolean isBuy() {
        return "BU".equals(this.transactionType);
    }

    /**
     * Check if transaction is a sell
     */
    public boolean isSell() {
        return "SL".equals(this.transactionType);
    }

    /**
     * Check if transaction is a transfer
     */
    public boolean isTransfer() {
        return "TR".equals(this.transactionType);
    }

    /**
     * Check if transaction is a fee
     */
    public boolean isFee() {
        return "FE".equals(this.transactionType);
    }

    /**
     * Check if transaction is pending
     */
    public boolean isPending() {
        return "P".equals(this.status);
    }

    /**
     * Check if transaction is done
     */
    public boolean isDone() {
        return "D".equals(this.status);
    }

    /**
     * Check if transaction failed
     */
    public boolean isFailed() {
        return "F".equals(this.status);
    }

    /**
     * Check if transaction is reversed
     */
    public boolean isReversed() {
        return "R".equals(this.status);
    }
}
