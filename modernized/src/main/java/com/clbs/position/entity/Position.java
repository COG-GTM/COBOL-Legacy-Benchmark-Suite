package com.clbs.position.entity;

import com.clbs.position.domain.PositionState;
import com.clbs.position.domain.PositionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Position master entity &mdash; the modern table replacement for the VSAM KSDS
 * {@code POSMSTR} whose record layout is copybook
 * {@code src/copybook/common/POSREC.cpy}:
 *
 * <pre>
 *   01  POSITION-RECORD.
 *       05  POS-KEY.
 *           10  POS-PORTFOLIO-ID   PIC X(08).
 *           10  POS-DATE           PIC X(08).
 *           10  POS-INVESTMENT-ID  PIC X(10).
 *       05  POS-DATA.
 *           10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *           10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3.
 *           10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3.
 *           10  POS-CURRENCY       PIC X(03).
 *           10  POS-STATUS         PIC X(01).
 *       05  POS-AUDIT.
 *           10  POS-LAST-MAINT-DATE   PIC X(26).
 *           10  POS-LAST-MAINT-USER   PIC X(08).
 * </pre>
 *
 * <p>The VSAM composite key {@code POS-KEY} is preserved as a unique constraint;
 * a surrogate {@code id} is used as the JPA primary key so the REST resource can
 * be addressed by a single path variable (modernization decision, see README).
 * {@code POS-FILLER PIC X(50)} is padding and is intentionally not mapped.</p>
 */
@Entity
@Table(name = "position_master",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_position_key",
                columnNames = {"portfolio_id", "position_date", "investment_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    /** Surrogate primary key (modernization of the VSAM composite key). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code POS-PORTFOLIO-ID PIC X(08)} &mdash; portfolio identifier. */
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** {@code POS-DATE PIC X(08)} &mdash; position date (YYYYMMDD). */
    @Column(name = "position_date", length = 8, nullable = false)
    private String positionDate;

    /** {@code POS-INVESTMENT-ID PIC X(10)} &mdash; investment identifier. */
    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    /** {@code POS-QUANTITY PIC S9(11)V9(4) COMP-3} &mdash; holding quantity. */
    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantity;

    /** {@code POS-COST-BASIS PIC S9(13)V9(2) COMP-3} &mdash; total cost basis. */
    @Column(name = "cost_basis", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBasis;

    /** {@code POS-MARKET-VALUE PIC S9(13)V9(2) COMP-3} &mdash; market value. */
    @Column(name = "market_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal marketValue;

    /** {@code POS-CURRENCY PIC X(03)} &mdash; ISO currency code. */
    @Column(name = "currency", length = 3)
    private String currency;

    /** {@code POS-STATUS PIC X(01)} &mdash; A=Active, C=Closed, P=Pending. */
    @Column(name = "status", length = 1, nullable = false)
    private String status;

    /** {@code POS-LAST-MAINT-DATE PIC X(26)} &mdash; last maintenance timestamp. */
    @Column(name = "last_maint_date", length = 26)
    private String lastMaintDate;

    /** {@code POS-LAST-MAINT-USER PIC X(08)} &mdash; last maintenance user. */
    @Column(name = "last_maint_user", length = 8)
    private String lastMaintUser;

    /** Snapshot of the calculable fields for use by {@code PositionCalculator}. */
    public PositionState toState() {
        return PositionState.of(quantity, costBasis, marketValue);
    }

    /** Applies a calculated holding back onto the entity. */
    public void applyState(PositionState state) {
        this.quantity = state.quantity();
        this.costBasis = state.costBasis();
        this.marketValue = state.marketValue();
    }

    public PositionStatus statusEnum() {
        return PositionStatus.fromCode(status);
    }
}
