package com.coggtm.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JPA entity mapped from DB2 table POSHIST (src/database/db2/POSHIST.sql).
 *
 * <p>Stores all portfolio transaction history with full cost-basis
 * and gain/loss tracking for financial auditing.</p>
 */
@Entity
@Table(name = "POSHIST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 8)
    @Column(name = "ACCOUNT_NO", length = 8, nullable = false)
    private String accountNo;

    @NotNull
    @Size(max = 10)
    @Column(name = "PORTFOLIO_ID", length = 10, nullable = false)
    private String portfolioId;

    @NotNull
    @Column(name = "TRANS_DATE", nullable = false)
    private LocalDate transDate;

    @NotNull
    @Column(name = "TRANS_TIME", nullable = false)
    private LocalTime transTime;

    @NotNull
    @Size(max = 2)
    @Column(name = "TRANS_TYPE", length = 2, nullable = false)
    private String transType;

    @NotNull
    @Size(max = 12)
    @Column(name = "SECURITY_ID", length = 12, nullable = false)
    private String securityId;

    @NotNull
    @Column(name = "QUANTITY", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    @NotNull
    @Column(name = "PRICE", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    @NotNull
    @Column(name = "AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Column(name = "FEES", precision = 15, scale = 2, nullable = false)
    private BigDecimal fees;

    @NotNull
    @Column(name = "TOTAL_AMOUNT", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @NotNull
    @Column(name = "COST_BASIS", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    @NotNull
    @Column(name = "GAIN_LOSS", precision = 15, scale = 2, nullable = false)
    private BigDecimal gainLoss;

    @NotNull
    @Column(name = "PROCESS_DATE", nullable = false)
    private LocalDate processDate;

    @NotNull
    @Column(name = "PROCESS_TIME", nullable = false)
    private LocalTime processTime;

    @NotNull
    @Size(max = 8)
    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    @NotNull
    @Size(max = 8)
    @Column(name = "USER_ID", length = 8, nullable = false)
    private String userId;

    @Column(name = "AUDIT_TIMESTAMP")
    private LocalDateTime auditTimestamp;
}
