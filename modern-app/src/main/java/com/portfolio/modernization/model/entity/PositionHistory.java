package com.portfolio.modernization.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "position_history", indexes = {
    @Index(name = "idx_poshist_security", columnList = "security_id, transaction_date"),
    @Index(name = "idx_poshist_process", columnList = "process_date, program_id")
})
@IdClass(PositionHistoryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionHistory {

    @Id
    @Column(name = "account_number", length = 8)
    private String accountNumber;

    @Id
    @Column(name = "portfolio_id", length = 10)
    private String portfolioId;

    @Id
    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Id
    @Column(name = "transaction_time")
    private LocalTime transactionTime;

    @Column(name = "transaction_type", length = 2, nullable = false)
    private String transactionType;

    @Column(name = "security_id", length = 12, nullable = false)
    private String securityId;

    @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "fees", precision = 15, scale = 2)
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

    @CreationTimestamp
    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;
}
