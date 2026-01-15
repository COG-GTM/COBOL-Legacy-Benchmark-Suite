package com.portfolio.modernization.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Transaction Record Entity
 * 
 * Modernized from COBOL copybook: src/copybook/common/TRNREC.cpy
 * Maps to database table: TRANSACTION_HISTORY
 * 
 * Original COBOL structure:
 * <pre>
 * 01  TRANSACTION-RECORD.
 *     05  TRN-KEY.
 *         10  TRN-DATE           PIC X(08).
 *         10  TRN-TIME           PIC X(06).
 *         10  TRN-PORTFOLIO-ID   PIC X(08).
 *         10  TRN-SEQUENCE-NO    PIC X(06).
 *     05  TRN-DATA.
 *         10  TRN-INVESTMENT-ID  PIC X(10).
 *         10  TRN-TYPE           PIC X(02).
 *         10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *         10  TRN-PRICE          PIC S9(11)V9(4) COMP-3.
 *         10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3.
 *         10  TRN-CURRENCY       PIC X(03).
 *         10  TRN-STATUS         PIC X(01).
 *     05  TRN-AUDIT.
 *         10  TRN-PROCESS-DATE   PIC X(26).
 *         10  TRN-PROCESS-USER   PIC X(08).
 * </pre>
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Entity
@Table(name = "TRANSACTION_HISTORY", indexes = {
    @Index(name = "IDX_TRN_PORTFOLIO_DATE", columnList = "portfolioId, transactionDate"),
    @Index(name = "IDX_TRN_DATE_TYPE", columnList = "transactionDate, transactionType"),
    @Index(name = "IDX_TRN_INVESTMENT", columnList = "investmentId, transactionDate"),
    @Index(name = "IDX_TRN_STATUS", columnList = "status, processDate")
})
public class TransactionRecord {

    /**
     * Transaction type constants (from TRN-TYPE 88-level conditions)
     */
    public static final String TYPE_BUY = "BUY";
    public static final String TYPE_SELL = "SELL";
    public static final String TYPE_TRANSFER = "TRANSFER";
    public static final String TYPE_FEE = "FEE";
    
    /**
     * Legacy COBOL transaction type codes
     */
    public static final String LEGACY_TYPE_BUY = "BU";
    public static final String LEGACY_TYPE_SELL = "SL";
    public static final String LEGACY_TYPE_TRANSFER = "TR";
    public static final String LEGACY_TYPE_FEE = "FE";

    /**
     * Transaction status constants (from TRN-STATUS 88-level conditions)
     */
    public static final String STATUS_PENDING = "P";
    public static final String STATUS_DONE = "D";
    public static final String STATUS_FAILED = "F";
    public static final String STATUS_REVERSED = "R";

    @Id
    @Column(name = "TRANSACTION_ID", length = 30, nullable = false)
    private String transactionId;

    @NotNull(message = "Portfolio ID is required")
    @Size(max = 20, message = "Portfolio ID cannot exceed 20 characters")
    @Column(name = "PORTFOLIO_ID", length = 20, nullable = false)
    private String portfolioId;

    @NotNull(message = "Transaction date is required")
    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "TRANSACTION_TIME")
    private LocalTime transactionTime;

    @NotNull(message = "Transaction type is required")
    @Size(max = 10, message = "Transaction type cannot exceed 10 characters")
    @Column(name = "TRANSACTION_TYPE", length = 10, nullable = false)
    private String transactionType;

    @Size(max = 10, message = "Investment ID cannot exceed 10 characters")
    @Column(name = "INVESTMENT_ID", length = 10)
    private String investmentId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", message = "Amount must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Amount precision exceeded")
    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @DecimalMin(value = "0.0000", message = "Units must be non-negative")
    @Digits(integer = 11, fraction = 4, message = "Units precision exceeded")
    @Column(name = "UNITS", precision = 15, scale = 4)
    private BigDecimal units;

    @DecimalMin(value = "0.0000", message = "Price must be non-negative")
    @Digits(integer = 11, fraction = 4, message = "Price precision exceeded")
    @Column(name = "PRICE", precision = 15, scale = 4)
    private BigDecimal price;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode = "USD";

    @Size(max = 1, message = "Status must be 1 character")
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status = STATUS_PENDING;

    @Size(max = 6, message = "Sequence number cannot exceed 6 characters")
    @Column(name = "SEQUENCE_NO", length = 6)
    private String sequenceNo;

    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDateTime processDate;

    @Size(max = 8, message = "Process user cannot exceed 8 characters")
    @Column(name = "PROCESS_USER", length = 8, nullable = false)
    private String processUser;

    @Column(name = "VSAM_MIGRATION_DATE")
    private LocalDateTime vsamMigrationDate;

    @Size(max = 28, message = "VSAM record key cannot exceed 28 characters")
    @Column(name = "VSAM_RECORD_KEY", length = 28)
    private String vsamRecordKey;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public TransactionRecord() {
        this.processDate = LocalDateTime.now();
    }

    public TransactionRecord(String portfolioId, LocalDate transactionDate, 
                            String transactionType, BigDecimal amount) {
        this();
        this.portfolioId = portfolioId;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
        this.amount = amount;
        generateTransactionId();
    }

    @PrePersist
    public void prePersist() {
        if (transactionId == null) {
            generateTransactionId();
        }
        if (processDate == null) {
            processDate = LocalDateTime.now();
        }
    }

    /**
     * Generates transaction ID from key components (mimics COBOL TRN-KEY structure)
     * Format: YYYYMMDDHHMMSS + PORTFOLIO_ID + SEQUENCE_NO
     */
    public void generateTransactionId() {
        StringBuilder sb = new StringBuilder();
        
        if (transactionDate != null) {
            sb.append(transactionDate.format(DateTimeFormatter.BASIC_ISO_DATE));
        } else {
            sb.append(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        
        if (transactionTime != null) {
            sb.append(transactionTime.format(DateTimeFormatter.ofPattern("HHmmss")));
        } else {
            sb.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
        }
        
        if (portfolioId != null) {
            sb.append(String.format("%-8s", portfolioId).substring(0, Math.min(8, portfolioId.length())));
        }
        
        if (sequenceNo != null) {
            sb.append(String.format("%6s", sequenceNo).replace(' ', '0'));
        } else {
            sb.append("000001");
        }
        
        this.transactionId = sb.toString();
    }

    /**
     * Converts legacy COBOL transaction type code to modern format
     * @param legacyType COBOL 2-character type code (BU, SL, TR, FE)
     * @return Modern transaction type string
     */
    public static String convertLegacyType(String legacyType) {
        if (legacyType == null) return null;
        return switch (legacyType.trim().toUpperCase()) {
            case "BU" -> TYPE_BUY;
            case "SL" -> TYPE_SELL;
            case "TR" -> TYPE_TRANSFER;
            case "FE" -> TYPE_FEE;
            default -> legacyType;
        };
    }

    /**
     * Converts modern transaction type to legacy COBOL format
     * @return COBOL 2-character type code
     */
    public String toLegacyType() {
        if (transactionType == null) return null;
        return switch (transactionType.trim().toUpperCase()) {
            case "BUY" -> LEGACY_TYPE_BUY;
            case "SELL" -> LEGACY_TYPE_SELL;
            case "TRANSFER" -> LEGACY_TYPE_TRANSFER;
            case "FEE" -> LEGACY_TYPE_FEE;
            default -> transactionType.length() >= 2 ? transactionType.substring(0, 2) : transactionType;
        };
    }

    /**
     * Validates transaction based on business rules from TRNVAL00.cbl
     * @return true if transaction is valid
     */
    public boolean isValidTransaction() {
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            return false;
        }
        if (transactionDate == null) {
            return false;
        }
        if (transactionType == null || transactionType.trim().isEmpty()) {
            return false;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (!isValidTransactionType()) {
            return false;
        }
        if (!isValidStatus()) {
            return false;
        }
        return true;
    }

    /**
     * Checks if transaction type is valid
     */
    public boolean isValidTransactionType() {
        if (transactionType == null) return false;
        String type = transactionType.trim().toUpperCase();
        return type.equals(TYPE_BUY) || type.equals(TYPE_SELL) || 
               type.equals(TYPE_TRANSFER) || type.equals(TYPE_FEE) ||
               type.equals(LEGACY_TYPE_BUY) || type.equals(LEGACY_TYPE_SELL) ||
               type.equals(LEGACY_TYPE_TRANSFER) || type.equals(LEGACY_TYPE_FEE);
    }

    /**
     * Checks if status is valid
     */
    public boolean isValidStatus() {
        if (status == null) return false;
        return status.equals(STATUS_PENDING) || status.equals(STATUS_DONE) ||
               status.equals(STATUS_FAILED) || status.equals(STATUS_REVERSED);
    }

    /**
     * Checks if transaction is a buy transaction
     */
    public boolean isBuyTransaction() {
        return TYPE_BUY.equalsIgnoreCase(transactionType) || 
               LEGACY_TYPE_BUY.equalsIgnoreCase(transactionType);
    }

    /**
     * Checks if transaction is a sell transaction
     */
    public boolean isSellTransaction() {
        return TYPE_SELL.equalsIgnoreCase(transactionType) || 
               LEGACY_TYPE_SELL.equalsIgnoreCase(transactionType);
    }

    /**
     * Checks if transaction is pending
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /**
     * Checks if transaction is completed
     */
    public boolean isCompleted() {
        return STATUS_DONE.equals(status);
    }

    /**
     * Checks if transaction failed
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    /**
     * Checks if transaction is reversed
     */
    public boolean isReversed() {
        return STATUS_REVERSED.equals(status);
    }

    /**
     * Calculates total transaction value (units * price)
     * @return calculated total or amount if units/price not available
     */
    public BigDecimal calculateTotalValue() {
        if (units != null && price != null) {
            return units.multiply(price);
        }
        return amount;
    }

    /**
     * Marks transaction as completed
     */
    public void markCompleted() {
        this.status = STATUS_DONE;
        this.processDate = LocalDateTime.now();
    }

    /**
     * Marks transaction as failed
     */
    public void markFailed() {
        this.status = STATUS_FAILED;
        this.processDate = LocalDateTime.now();
    }

    /**
     * Marks transaction as reversed
     */
    public void markReversed() {
        this.status = STATUS_REVERSED;
        this.processDate = LocalDateTime.now();
    }

    /**
     * Creates VSAM record key for migration tracking
     * Format: TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO
     */
    public String createVsamRecordKey() {
        StringBuilder sb = new StringBuilder();
        if (transactionDate != null) {
            sb.append(transactionDate.format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        if (transactionTime != null) {
            sb.append(transactionTime.format(DateTimeFormatter.ofPattern("HHmmss")));
        }
        if (portfolioId != null) {
            sb.append(String.format("%-8s", portfolioId));
        }
        if (sequenceNo != null) {
            sb.append(String.format("%-6s", sequenceNo));
        }
        return sb.toString();
    }

    // Getters and Setters

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public LocalDateTime getProcessDate() {
        return processDate;
    }

    public void setProcessDate(LocalDateTime processDate) {
        this.processDate = processDate;
    }

    public String getProcessUser() {
        return processUser;
    }

    public void setProcessUser(String processUser) {
        this.processUser = processUser;
    }

    public LocalDateTime getVsamMigrationDate() {
        return vsamMigrationDate;
    }

    public void setVsamMigrationDate(LocalDateTime vsamMigrationDate) {
        this.vsamMigrationDate = vsamMigrationDate;
    }

    public String getVsamRecordKey() {
        return vsamRecordKey;
    }

    public void setVsamRecordKey(String vsamRecordKey) {
        this.vsamRecordKey = vsamRecordKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionRecord that = (TransactionRecord) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "TransactionRecord{" +
                "transactionId='" + transactionId + '\'' +
                ", portfolioId='" + portfolioId + '\'' +
                ", transactionDate=" + transactionDate +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                '}';
    }
}
