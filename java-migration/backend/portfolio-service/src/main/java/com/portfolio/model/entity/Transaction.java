package com.portfolio.model.entity;

import com.portfolio.model.enums.TransactionStatus;
import com.portfolio.model.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "transaction_time")
    private LocalTime transactionTime;

    @NotBlank
    @Size(max = 8)
    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    @Size(max = 6)
    @Column(name = "sequence_no", length = 6)
    private String sequenceNo;

    @Size(max = 10)
    @Column(name = "investment_id", length = 10)
    private String investmentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @NotNull
    @Positive
    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Size(max = 3)
    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TransactionStatus status;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Size(max = 8)
    @Column(name = "process_user", length = 8)
    private String processUser;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
        if (transactionTime == null) {
            transactionTime = LocalTime.now();
        }
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
        if (currency == null) {
            currency = "USD";
        }
    }
}
