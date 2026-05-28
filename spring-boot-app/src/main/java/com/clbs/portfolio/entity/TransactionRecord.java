package com.clbs.portfolio.entity;

import com.clbs.portfolio.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trn_date", length = 8, nullable = false)
    private String trnDate;

    @Column(name = "trn_time", length = 6, nullable = false)
    private String trnTime;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "sequence_no", length = 6, nullable = false)
    private String sequenceNo;

    @Column(name = "investment_id", length = 10)
    private String investmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trn_type", length = 2, nullable = false)
    private TransactionType trnType;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status", length = 10, nullable = false)
    private String status;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8)
    private String processUser;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "adjudication_status", length = 20)
    private String adjudicationStatus;

    @Column(name = "fee_amount", precision = 18, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "settlement_amount", precision = 18, scale = 2)
    private BigDecimal settlementAmount;

    @Column(name = "cost_basis_adjustment", precision = 18, scale = 2)
    private BigDecimal costBasisAdjustment;
}
