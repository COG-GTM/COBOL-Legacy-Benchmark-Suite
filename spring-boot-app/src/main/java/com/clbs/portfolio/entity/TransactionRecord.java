package com.clbs.portfolio.entity;

import com.clbs.portfolio.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "transaction_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {

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
    @Column(name = "transaction_type", length = 10, nullable = false)
    private TransactionType transactionType;

    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status", length = 1)
    private String status;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8)
    private String processUser;
}
