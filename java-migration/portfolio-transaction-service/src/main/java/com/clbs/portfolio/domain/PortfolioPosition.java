package com.clbs.portfolio.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Portfolio master / position record updated by PORTTRAN. This is the VSAM
 * {@code PORTFOLIO-FILE} record (an indexed KSDS keyed by {@code PORT-ID}),
 * accessed via keyed READ and REWRITE.
 *
 * <p><b>Reconstruction note:</b> PORTTRAN copies a copybook named {@code PORTREC},
 * which is <i>not present</i> in the repository. The fields below are reconstructed
 * from how PORTTRAN actually uses the record:</p>
 * <ul>
 *   <li>{@code PORT-ID}          — RECORD KEY (see {@code SELECT ... RECORD KEY IS PORT-ID})</li>
 *   <li>{@code PORT-ACCOUNT-NO}  — moved into the audit record ({@code AUD-ACCOUNT-NO})</li>
 *   <li>{@code PORT-TOTAL-UNITS} — target of {@code ADD/SUBTRACT TRN-QUANTITY}; the matching
 *       transaction field is {@code S9(11)V9(4) COMP-3}, so this is modeled at scale 4</li>
 *   <li>{@code PORT-TOTAL-COST}  — target of {@code ADD/SUBTRACT TRN-AMOUNT}; the matching
 *       transaction field is {@code S9(13)V9(2) COMP-3}, so this is modeled at scale 2</li>
 * </ul>
 *
 * <p>The closest documented layout, {@code PORTFLIO.cpy} ({@code PORT-RECORD}),
 * uses {@code PORT-TOTAL-VALUE}/{@code PORT-CASH-BALANCE} rather than
 * {@code PORT-TOTAL-UNITS}/{@code PORT-TOTAL-COST}; this divergence is documented
 * in the modernization note.</p>
 */
@Entity
@Table(name = "portfolio_position")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPosition {

    /** {@code PORT-ID PIC X(8)} — portfolio identifier and primary (record) key. */
    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** {@code PORT-ACCOUNT-NO PIC X(10)} — account number. */
    @Column(name = "account_no", length = 10)
    private String accountNo;

    /**
     * {@code PORT-TOTAL-UNITS PIC S9(11)V9(4) COMP-3} — total held units
     * (reconstructed; scale 4 to match {@code TRN-QUANTITY}).
     */
    @Column(name = "total_units", precision = CobolDecimal.TOTAL_DIGITS, scale = CobolDecimal.QUANTITY_SCALE)
    private BigDecimal totalUnits;

    /**
     * {@code PORT-TOTAL-COST PIC S9(13)V9(2) COMP-3} — total cost basis
     * (reconstructed; scale 2 to match {@code TRN-AMOUNT}).
     */
    @Column(name = "total_cost", precision = CobolDecimal.TOTAL_DIGITS, scale = CobolDecimal.AMOUNT_SCALE)
    private BigDecimal totalCost;
}
