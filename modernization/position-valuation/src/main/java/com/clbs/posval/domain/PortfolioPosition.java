package com.clbs.posval.domain;

import com.clbs.posval.cobol.PackedField;
import java.math.BigDecimal;

/**
 * The portfolio-level holding that {@code PORTTRAN} 2210/2220/2240 mutate:
 * {@code PORT-TOTAL-UNITS} and {@code PORT-TOTAL-COST}.
 *
 * <p><b>Reconstructed layout.</b> {@code PORTTRAN} declares its portfolio file with
 * {@code COPY PORTREC}, and no {@code PORTREC} copybook exists anywhere in the repository, so the
 * PIC clauses of these two fields are not recoverable from source. This port uses the widths of
 * the equivalent fields in the copybooks that do exist — {@code POS-QUANTITY PIC S9(11)V9(4)
 * COMP-3} and {@code POS-COST-BASIS PIC S9(13)V9(2) COMP-3} of POSREC — which are also the widths
 * of the transaction operands {@code TRN-QUANTITY} and {@code TRN-AMOUNT} added to them. This is
 * spec open question OQ-1 and is the one assumption in the slice that a mainframe owner must
 * confirm against the production copybook library, because it fixes where money arithmetic
 * truncates.
 *
 * @param portfolioId {@code PORT-ID}, the record key
 * @param accountNo {@code PORT-ACCOUNT-NO}, carried into the audit record
 * @param totalUnits {@code PORT-TOTAL-UNITS}, assumed {@code PIC S9(11)V9(4) COMP-3}
 * @param totalCost {@code PORT-TOTAL-COST}, assumed {@code PIC S9(13)V9(2) COMP-3}
 */
public record PortfolioPosition(
        String portfolioId, String accountNo, BigDecimal totalUnits, BigDecimal totalCost) {

    public PortfolioPosition {
        totalUnits = PackedField.QUANTITY.store(totalUnits);
        totalCost = PackedField.AMOUNT.store(totalCost);
    }

    public static PortfolioPosition of(String portfolioId, BigDecimal totalUnits, BigDecimal totalCost) {
        return new PortfolioPosition(portfolioId, "0000000000", totalUnits, totalCost);
    }

    public PortfolioPosition withTotals(BigDecimal newUnits, BigDecimal newCost) {
        return new PortfolioPosition(portfolioId, accountNo, newUnits, newCost);
    }

    /** The 100 byte {@code AUD-BEFORE-IMAGE} / {@code AUD-AFTER-IMAGE} rendering of the record. */
    public String image() {
        return "%-8s%-10s%s|%s".formatted(portfolioId, accountNo, totalUnits, totalCost);
    }
}
