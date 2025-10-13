package com.coggtm.clbs.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_trn_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_trn_transaction_date", columnList = "transaction_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotNull
    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    @NotNull
    @Size(max = 10)
    @Column(name = "portfolio_id", nullable = false, length = 10)
    private String portfolioId;

    @NotNull
    @Size(max = 6)
    @Column(name = "sequence_number", nullable = false, length = 6)
    private String sequenceNumber;

    @NotNull
    @Size(max = 10)
    @Column(name = "investment_id", nullable = false, length = 10)
    private String investmentId;

    @NotNull
    @Size(max = 4)
    @Column(name = "transaction_type", nullable = false, length = 4)
    private String transactionType;

    @NotNull
    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @NotNull
    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Size(max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @Size(max = 1)
    @Column(name = "status", nullable = false, length = 1)
    private String status;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Size(max = 8)
    @Column(name = "process_user", length = 8)
    private String processUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
