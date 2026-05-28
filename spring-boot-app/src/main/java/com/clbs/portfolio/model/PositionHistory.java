package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 * Position history table (DB2 POSHIST).
 * From COBOL copybook: src/copybook/db2/DBTBLS.cpy (POSHIST-RECORD)
 * and SQL: src/database/db2/POSHIST.sql.
 */
@Entity
@Table(name = "position_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** PH-ACCOUNT-NO — CHAR(8) */
    @Column(name = "account_no", length = 8, nullable = false)
    private String accountNo;

    /** PH-PORTFOLIO-ID — CHAR(10) */
    @Column(name = "portfolio_id", length = 10, nullable = false)
    private String portfolioId;

    /** PH-TRANS-DATE — DATE */
    @Column(name = "trans_date", nullable = false)
    private LocalDate transDate;

    /** PH-TRANS-TIME — TIME */
    @Column(name = "trans_time", nullable = false)
    private LocalTime transTime;

    /** PH-TRANS-TYPE — CHAR(2) */
    @Column(name = "trans_type", length = 2, nullable = false)
    private String transType;

    /** PH-SECURITY-ID — CHAR(12) */
    @Column(name = "security_id", length = 12, nullable = false)
    private String securityId;

    /** PH-QUANTITY — DECIMAL(15,3) */
    @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    /** PH-PRICE — DECIMAL(15,3) */
    @Column(name = "price", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    /** PH-AMOUNT — DECIMAL(15,2) */
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** PH-FEES — DECIMAL(15,2) DEFAULT 0 */
    @Column(name = "fees", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;

    /** PH-TOTAL-AMOUNT — DECIMAL(15,2) */
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** PH-COST-BASIS — DECIMAL(15,2) */
    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** PH-GAIN-LOSS — DECIMAL(15,2) */
    @Column(name = "gain_loss", precision = 15, scale = 2, nullable = false)
    private BigDecimal gainLoss;

    /** PH-PROCESS-DATE — DATE */
    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    /** PH-PROCESS-TIME — TIME */
    @Column(name = "process_time", nullable = false)
    private LocalTime processTime;

    /** PH-PROGRAM-ID — CHAR(8) */
    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    /** PH-USER-ID — CHAR(8) */
    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    /** PH-AUDIT-TIMESTAMP — TIMESTAMP */
    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;
}
