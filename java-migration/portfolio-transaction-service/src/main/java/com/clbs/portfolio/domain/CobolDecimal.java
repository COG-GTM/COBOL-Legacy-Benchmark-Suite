package com.clbs.portfolio.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decimal conventions that reproduce COBOL fixed-point (COMP-3 / packed decimal)
 * storage semantics for the PORTTRAN record layouts.
 *
 * <p>COBOL fields used by PORTTRAN:</p>
 * <ul>
 *   <li>{@code TRN-QUANTITY  PIC S9(11)V9(4) COMP-3} &rarr; scale 4, 15 total digits</li>
 *   <li>{@code TRN-PRICE     PIC S9(11)V9(4) COMP-3} &rarr; scale 4, 15 total digits</li>
 *   <li>{@code TRN-AMOUNT    PIC S9(13)V9(2) COMP-3} &rarr; scale 2, 15 total digits</li>
 * </ul>
 *
 * <p><b>Rounding parity:</b> COBOL {@code MOVE}/{@code COMPUTE} without the
 * {@code ROUNDED} phrase <i>truncates</i> excess fractional digits toward zero.
 * PORTTRAN never specifies {@code ROUNDED}, so we mirror that with
 * {@link RoundingMode#DOWN} when normalizing values into a fixed scale.</p>
 */
public final class CobolDecimal {

    /** Scale of quantity/units fields: {@code V9(4)}. */
    public static final int QUANTITY_SCALE = 4;

    /** Scale of monetary (amount/cost) fields: {@code V9(2)}. */
    public static final int AMOUNT_SCALE = 2;

    /** Total digit count of the COMP-3 fields used here (S9(13)V9(2) / S9(11)V9(4)). */
    public static final int TOTAL_DIGITS = 15;

    private CobolDecimal() {
    }

    /**
     * Normalize a value to a COBOL field scale using truncation (RoundingMode.DOWN),
     * mirroring how a value is stored into a packed-decimal field without ROUNDED.
     *
     * @param value the value to normalize (null becomes ZERO at the requested scale)
     * @param scale the target fractional scale
     * @return the value truncated to {@code scale} fractional digits
     */
    public static BigDecimal normalize(BigDecimal value, int scale) {
        BigDecimal v = (value == null) ? BigDecimal.ZERO : value;
        return v.setScale(scale, RoundingMode.DOWN);
    }

    /** Normalize to quantity scale (4). */
    public static BigDecimal quantity(BigDecimal value) {
        return normalize(value, QUANTITY_SCALE);
    }

    /** Normalize to amount/money scale (2). */
    public static BigDecimal amount(BigDecimal value) {
        return normalize(value, AMOUNT_SCALE);
    }
}
