package com.clbs.position.domain;

import java.math.RoundingMode;

/**
 * Fixed-point decimal scales derived directly from the COBOL PIC clauses, so
 * that the Java {@link java.math.BigDecimal} arithmetic preserves the exact
 * precision of the legacy mainframe fields. Floating-point types are never used
 * for monetary values (see knowledge note "COBOL-to-Modern-Language Migration
 * Patterns": NEVER use floating-point for financial data).
 *
 * <ul>
 *   <li>{@code QUANTITY_SCALE = 4} &mdash; {@code POS-QUANTITY}/{@code TRN-QUANTITY PIC S9(11)V9(4)}</li>
 *   <li>{@code PRICE_SCALE = 4} &mdash; {@code TRN-PRICE PIC S9(11)V9(4)}</li>
 *   <li>{@code AMOUNT_SCALE = 2} &mdash; {@code POS-COST-BASIS}/{@code POS-MARKET-VALUE}/{@code TRN-AMOUNT PIC S9(13)V9(2)}</li>
 * </ul>
 *
 * <p>COBOL {@code COMPUTE} without the {@code ROUNDED} phrase truncates, but
 * additions and subtractions of equal-scale fields (the buy/sell aggregation)
 * are exact and need no rounding. The only division is the average-cost
 * computation; there we carry extra precision and round the final monetary
 * result {@link RoundingMode#HALF_UP} for financial correctness.</p>
 */
public final class MoneyScale {

    private MoneyScale() {
    }

    public static final int QUANTITY_SCALE = 4;
    public static final int PRICE_SCALE = 4;
    public static final int AMOUNT_SCALE = 2;

    /** Extra precision used for intermediate average-cost (unit cost) values. */
    public static final int UNIT_COST_SCALE = 8;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
}
