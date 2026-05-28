package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Investment position record.
 * From COBOL copybook: src/copybook/common/POSREC.cpy (POSITION-RECORD).
 */
@Entity
@Table(name = "position")
@IdClass(PositionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    /** POS-PORTFOLIO-ID — PIC X(08) */
    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** POS-DATE — PIC X(08), YYYYMMDD */
    @Id
    @Column(name = "pos_date", length = 8, nullable = false)
    private String posDate;

    /** POS-INVESTMENT-ID — PIC X(10) */
    @Id
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    /** POS-QUANTITY — PIC S9(11)V9(4) COMP-3 */
    @Column(name = "quantity", precision = 15, scale = 4)
    private BigDecimal quantity;

    /** POS-COST-BASIS — PIC S9(13)V9(2) COMP-3 */
    @Column(name = "cost_basis", precision = 15, scale = 2)
    private BigDecimal costBasis;

    /** POS-MARKET-VALUE — PIC S9(13)V9(2) COMP-3 */
    @Column(name = "market_value", precision = 15, scale = 2)
    private BigDecimal marketValue;

    /** POS-CURRENCY — PIC X(03) */
    @Column(name = "currency", length = 3)
    private String currency;

    /** POS-STATUS — PIC X(01): A=Active, C=Closed, P=Pending */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private PositionStatus status;

    /** POS-LAST-MAINT-DATE — PIC X(26) */
    @Column(name = "last_maint_date")
    private LocalDateTime lastMaintDate;

    /** POS-LAST-MAINT-USER — PIC X(08) */
    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;

    public enum PositionStatus {
        ACTIVE,
        CLOSED,
        PENDING
    }
}
