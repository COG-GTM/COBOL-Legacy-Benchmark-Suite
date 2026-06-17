package com.clbs.position.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Position history entity &mdash; the modern table replacement for the DB2 table
 * {@code POSHIST} ({@code src/database/db2/POSHIST.sql}, copybook
 * {@code src/copybook/db2/DBTBLS.cpy}). The COBOL position-update job recorded
 * every applied trade here together with its computed cost basis and realized
 * gain/loss (the "Records transaction history" responsibility of POSUPDT).
 *
 * <pre>
 *   CREATE TABLE POSHIST
 *     (ACCOUNT_NO     CHAR(8),     PORTFOLIO_ID  CHAR(10),
 *      TRANS_DATE     DATE,        TRANS_TIME    TIME,
 *      TRANS_TYPE     CHAR(2),     SECURITY_ID   CHAR(12),
 *      QUANTITY       DECIMAL(15,3), PRICE       DECIMAL(15,3),
 *      AMOUNT         DECIMAL(15,2), FEES        DECIMAL(15,2),
 *      TOTAL_AMOUNT   DECIMAL(15,2), COST_BASIS  DECIMAL(15,2),
 *      GAIN_LOSS      DECIMAL(15,2),
 *      PROCESS_DATE   DATE,        PROCESS_TIME  TIME,
 *      PROGRAM_ID     CHAR(8),     USER_ID       CHAR(8),
 *      AUDIT_TIMESTAMP TIMESTAMP);
 * </pre>
 *
 * <p>The DB2 partitioned-tablespace / clustered-index physical attributes do not
 * carry over; the logical column set and precision are preserved.</p>
 */
@Entity
@Table(name = "position_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code PH-ACCOUNT-NO CHAR(8)} &mdash; account number. */
    @Column(name = "account_no", length = 8, nullable = false)
    private String accountNo;

    /** {@code PH-PORTFOLIO-ID CHAR(10)} &mdash; portfolio identifier. */
    @Column(name = "portfolio_id", length = 10, nullable = false)
    private String portfolioId;

    /** {@code TRANS_DATE DATE} &mdash; transaction date. */
    @Column(name = "trans_date", nullable = false)
    private LocalDate transDate;

    /** {@code TRANS_TIME TIME} &mdash; transaction time. */
    @Column(name = "trans_time", nullable = false)
    private LocalTime transTime;

    /** {@code TRANS_TYPE CHAR(2)} &mdash; BU/SL/TR/FE. */
    @Column(name = "trans_type", length = 2, nullable = false)
    private String transType;

    /** {@code SECURITY_ID CHAR(12)} &mdash; security identifier. */
    @Column(name = "security_id", length = 12, nullable = false)
    private String securityId;

    /** {@code QUANTITY DECIMAL(15,3)} &mdash; transaction quantity. */
    @Column(name = "quantity", precision = 15, scale = 3, nullable = false)
    private BigDecimal quantity;

    /** {@code PRICE DECIMAL(15,3)} &mdash; transaction price. */
    @Column(name = "price", precision = 15, scale = 3, nullable = false)
    private BigDecimal price;

    /** {@code AMOUNT DECIMAL(15,2)} &mdash; gross amount. */
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /** {@code FEES DECIMAL(15,2)} &mdash; fees applied. */
    @Column(name = "fees", precision = 15, scale = 2, nullable = false)
    private BigDecimal fees;

    /** {@code TOTAL_AMOUNT DECIMAL(15,2)} &mdash; amount including fees. */
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** {@code COST_BASIS DECIMAL(15,2)} &mdash; resulting position cost basis. */
    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** {@code GAIN_LOSS DECIMAL(15,2)} &mdash; realized gain/loss for this trade. */
    @Column(name = "gain_loss", precision = 15, scale = 2, nullable = false)
    private BigDecimal gainLoss;

    /** {@code PROCESS_DATE DATE} &mdash; batch processing date. */
    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    /** {@code PROCESS_TIME TIME} &mdash; batch processing time. */
    @Column(name = "process_time", nullable = false)
    private LocalTime processTime;

    /** {@code PROGRAM_ID CHAR(8)} &mdash; producing program (POSUPD00). */
    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    /** {@code USER_ID CHAR(8)} &mdash; user/job identity. */
    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    /** {@code AUDIT_TIMESTAMP TIMESTAMP} &mdash; audit timestamp. */
    @Column(name = "audit_timestamp", nullable = false)
    private LocalDateTime auditTimestamp;
}
