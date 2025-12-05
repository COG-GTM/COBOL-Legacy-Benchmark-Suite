package com.portfolio.modernization.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "transaction", indexes = {
    @Index(name = "idx_transaction_portfolio", columnList = "portfolio_id, transaction_date"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date, portfolio_id"),
    @Index(name = "idx_transaction_investment", columnList = "investment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Column(name = "transaction_id", length = 28)
    private String transactionId;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    @Column(name = "sequence_number", length = 6)
    private String sequenceNumber;

    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "transaction_type", length = 2, nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "status", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "process_user", length = 8)
    private String processUser;

    @CreationTimestamp
    @Column(name = "process_timestamp", nullable = false)
    private LocalDateTime processTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", insertable = false, updatable = false)
    private Portfolio portfolio;

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
