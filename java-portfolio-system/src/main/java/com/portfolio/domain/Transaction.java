package com.portfolio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Transaction entity - migrated from COBOL copybook TRNREC.cpy
 * Represents financial transaction records
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time")
    private LocalTime transactionTime;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "sequence_no", length = 6)
    private String sequenceNo;

    @Column(name = "investment_id", length = 10)
    private String investmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 2)
    private TransactionType transactionType;

    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 1)
    private TransactionStatus status;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8)
    private String processUser;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
        if (transactionTime == null) {
            transactionTime = LocalTime.now();
        }
    }

    public enum TransactionType {
        BU, // Buy
        SL, // Sell
        TR, // Transfer
        FE  // Fee
    }

    public enum TransactionStatus {
        P, // Pending
        D, // Done
        F, // Failed
        R  // Reversed
    }
}
