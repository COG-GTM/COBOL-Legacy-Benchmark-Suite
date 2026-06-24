package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity mapped from COBOL copybook POSREC.cpy (Position Record).
 * <p>
 * COBOL record layout (VSAM KSDS, 350-byte fixed-length record):
 * <pre>
 * 01  POSITION-RECORD.
 *     05  POS-KEY.
 *         10  POS-PORTFOLIO-ID   PIC X(08)
 *         10  POS-DATE           PIC X(08)   [YYYYMMDD]
 *         10  POS-INVESTMENT-ID  PIC X(10)
 *     05  POS-DATA.
 *         10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3
 *         10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3
 *         10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3
 *         10  POS-CURRENCY       PIC X(03)
 *         10  POS-STATUS         PIC X(01)   [A=Active, C=Closed, P=Pending]
 *     05  POS-AUDIT.
 *         10  POS-LAST-MAINT-DATE   PIC X(26)
 *         10  POS-LAST-MAINT-USER   PIC X(08)
 *     05  POS-FILLER               PIC X(50)
 * </pre>
 */
@Entity
@Table(name = "position")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** POS-PORTFOLIO-ID — PIC X(08). Foreign key to portfolio. */
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** POS-DATE — PIC X(08). Position date (YYYYMMDD). */
    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    /** POS-INVESTMENT-ID — PIC X(10). Investment instrument identifier. */
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    /** POS-QUANTITY — PIC S9(11)V9(4) COMP-3. Holding quantity (4 decimal places). */
    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** POS-COST-BASIS — PIC S9(13)V9(2) COMP-3. Total cost basis. */
    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** POS-MARKET-VALUE — PIC S9(13)V9(2) COMP-3. Current market value. */
    @Column(name = "market_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue;

    /** POS-CURRENCY — PIC X(03). ISO 4217 currency code. */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /** POS-STATUS — PIC X(01). A=Active, C=Closed, P=Pending. */
    @Column(name = "status", length = 1, nullable = false)
    private String status;

    /** POS-LAST-MAINT-DATE — PIC X(26). Last maintenance timestamp. */
    @Column(name = "last_maint_date")
    private LocalDateTime lastMaintDate;

    /** POS-LAST-MAINT-USER — PIC X(08). Last user who modified. */
    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;
}
