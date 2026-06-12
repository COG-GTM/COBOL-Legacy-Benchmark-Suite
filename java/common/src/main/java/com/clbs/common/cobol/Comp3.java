package com.clbs.common.cobol;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Scale/rounding helpers for COBOL COMP-3 (packed decimal) fields mapped to
 * {@link BigDecimal}. COBOL fixed-point arithmetic truncates/rounds at a fixed
 * scale; these helpers keep Java values at the copybook-declared scale.
 */
public final class Comp3 {

    private Comp3() {
    }

    /** Scale for {@code S9(n)V99} money fields. */
    public static final int MONEY_SCALE = 2;

    /** Scale for {@code S9(n)V9(4)} quantity/price fields. */
    public static final int QUANTITY_SCALE = 4;

    /** Force a value to the given scale using COBOL-style HALF_UP rounding. */
    public static BigDecimal scale(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal money(BigDecimal value) {
        return scale(value, MONEY_SCALE);
    }

    public static BigDecimal quantity(BigDecimal value) {
        return scale(value, QUANTITY_SCALE);
    }
}
