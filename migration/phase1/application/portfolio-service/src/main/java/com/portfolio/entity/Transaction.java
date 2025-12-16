package com.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transaction entity - migrated from TRANHIST VSAM file.
 * 
 * Original COBOL structure from TRNREC.cpy:
 * - Key: Transaction Date (8 bytes) + Time (6 bytes) + Portfolio ID (8 bytes) + Sequence (6 bytes)
 * - Record Length: 300 bytes
 * 
 * Transaction Types (from COBOL):
 * - BU = Buy
 * - SL = Sell
 * - TR = Transfer
 * - FE = Fee
 * 
 * @see src/copybook/common/TRNREC.cpy
 */
@Entity
@Table(name = "transactions", schema = "portfolio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 20)
    private String transactionId;

    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    @Column(name = "sequence_no", nullable = false, length = 6)
    private String sequenceNo;

    @Column(name = "investment_id", nullable = false, length = 10)
    private String investmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fees", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "before_balance", precision = 15, scale = 4)
    private BigDecimal beforeBalance;

    @Column(name = "after_balance", precision = 15, scale = 4)
    private BigDecimal afterBalance;

    @Column(name = "result_code", length = 4)
    private String resultCode;

    @Column(name = "process_date")
    private OffsetDateTime processDate;

    @Column(name = "process_user", length = 8)
    private String processUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum TransactionType {
        BUY,
        SELL,
        TRANSFER,
        FEE
    }

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REVERSED
    }
}
