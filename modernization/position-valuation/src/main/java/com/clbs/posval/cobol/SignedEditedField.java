package com.clbs.posval.cobol;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The numeric-edited report field {@code WS-POS-CHANGE-PCT PIC +ZZ9.99} of
 * {@code RPTPOS00} (WORKING-STORAGE {@code WS-POSITION-DETAIL}).
 *
 * <p>The edit mask carries real business meaning, because it is the only place the percentage
 * change is ever materialised:
 *
 * <ul>
 *   <li>{@code +} — a fixed leading sign position; the sign is always printed, {@code +} or {@code -}.
 *   <li>{@code ZZ9} — three integer digit positions, the leading two zero-suppressed to spaces.
 *       A value of 1000% or more does not fit and, absent {@code ON SIZE ERROR}, its high-order
 *       digits are discarded, so 12245.67% prints as {@code +245.67}.
 *   <li>{@code .99} — two decimal digit positions, truncated toward zero (no {@code ROUNDED}).
 * </ul>
 *
 * <p>Total width is seven characters.
 */
public final class SignedEditedField {

    /** {@code PIC +ZZ9.99}: three integer digits, two decimals. */
    public static final PackedField PIC = new PackedField("+ZZ9.99", 3, 2);

    public static final int WIDTH = 7;

    private SignedEditedField() {}

    /** Applies the field's truncation rules to {@code value}, returning the stored numeric content. */
    public static BigDecimal store(BigDecimal value) {
        return PIC.store(value);
    }

    /**
     * Renders {@code value} the way the field appears in the report line, e.g. {@code "+ 10.00"},
     * {@code "+  0.00"}, {@code "-100.00"}.
     */
    public static String format(BigDecimal value) {
        BigDecimal stored = store(value);
        BigDecimal magnitude = stored.abs().setScale(2, RoundingMode.DOWN);
        String digits = magnitude.toPlainString();          // "0.00" .. "999.99"
        String integerPart = digits.substring(0, digits.indexOf('.'));
        String decimalPart = digits.substring(digits.indexOf('.') + 1);
        String suppressed = " ".repeat(3 - integerPart.length()) + integerPart;
        char sign = stored.signum() < 0 ? '-' : '+';
        return sign + suppressed + "." + decimalPart;
    }
}
