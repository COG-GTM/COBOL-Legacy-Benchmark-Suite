package com.clbs.posval.domain;

import com.clbs.posval.cobol.PackedField;
import java.math.BigDecimal;

/**
 * {@code POSITION-RECORD} of {@code src/copybook/common/POSREC.cpy}, the Position Master record.
 *
 * <pre>
 * 05 POS-KEY.
 *    10 POS-PORTFOLIO-ID    PIC X(08)
 *    10 POS-DATE            PIC X(08)              position date, YYYYMMDD
 *    10 POS-INVESTMENT-ID   PIC X(10)
 * 05 POS-DATA.
 *    10 POS-QUANTITY        PIC S9(11)V9(4) COMP-3
 *    10 POS-COST-BASIS      PIC S9(13)V9(2) COMP-3
 *    10 POS-MARKET-VALUE    PIC S9(13)V9(2) COMP-3
 *    10 POS-CURRENCY        PIC X(03)
 *    10 POS-STATUS          PIC X(01)              A=active, C=closed, P=pending
 * 05 POS-AUDIT.
 *    10 POS-LAST-MAINT-DATE PIC X(26)
 *    10 POS-LAST-MAINT-USER PIC X(08)
 * 05 POS-FILLER             PIC X(50)
 * </pre>
 *
 * <p>Note what is absent: no price field, and no previous-day value. Nothing in the slice computes
 * {@code POS-MARKET-VALUE}; every program either reports it or ignores it (spec open question
 * OQ-5).
 */
public record PositionRecord(
        String portfolioId,
        String positionDate,
        String investmentId,
        BigDecimal quantity,
        BigDecimal costBasis,
        BigDecimal marketValue,
        String currency,
        String status) {

    /** {@code POS-STATUS-ACTIVE VALUE 'A'}. */
    public static final String STATUS_ACTIVE = "A";
    /** {@code POS-STATUS-CLOSED VALUE 'C'}. */
    public static final String STATUS_CLOSED = "C";
    /** {@code POS-STATUS-PEND VALUE 'P'}. */
    public static final String STATUS_PENDING = "P";

    public PositionRecord {
        quantity = PackedField.QUANTITY.store(quantity);
        costBasis = PackedField.AMOUNT.store(costBasis);
        marketValue = PackedField.AMOUNT.store(marketValue);
    }

    /** {@code POS-KEY}: portfolio id, position date and investment id concatenated. */
    public String key() {
        return "%-8s%-8s%-10s".formatted(portfolioId, positionDate, investmentId);
    }
}
