package com.clbs.portfolio.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helpers reproducing COBOL packed-decimal (COMP-3) storage semantics.
 *
 * <p>A {@code PIC S9(i)V9(s) COMP-3} field holds exactly {@code i} integer digits and {@code s}
 * decimal digits. A value moved into such a field is truncated (never rounded, unless the COBOL
 * statement says {@code ROUNDED}) on the right, and high-order digits that do not fit are dropped
 * silently when the statement has no {@code ON SIZE ERROR} clause. Every arithmetic statement in
 * the translated slice is unrounded and unguarded, so both behaviours are reproduced here.
 */
public final class CobolDecimal {

    /** {@code PIC S9(11)V9(4)} - TRN-QUANTITY, TRN-PRICE, POS-QUANTITY. */
    public static final int QUANTITY_DIGITS = 11;
    public static final int QUANTITY_SCALE = 4;

    /** {@code PIC S9(13)V9(2)} - TRN-AMOUNT, POS-COST-BASIS, POS-MARKET-VALUE, PORT-TOTAL-VALUE. */
    public static final int AMOUNT_DIGITS = 13;
    public static final int AMOUNT_SCALE = 2;

    public static final BigDecimal ZERO_QUANTITY = BigDecimal.ZERO.setScale(QUANTITY_SCALE);
    public static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(AMOUNT_SCALE);

    private CobolDecimal() {
    }

    /**
     * Stores {@code value} as it would be held in a {@code PIC S9(digits)V9(scale)} field: null and
     * missing decimals become zeros, excess decimals are truncated toward zero and integer digits
     * beyond {@code digits} are dropped.
     */
    public static BigDecimal store(BigDecimal value, int digits, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale);
        }
        BigDecimal scaled = value.setScale(scale, RoundingMode.DOWN);
        BigDecimal limit = BigDecimal.TEN.pow(digits);
        if (scaled.abs().compareTo(limit) >= 0) {
            BigDecimal truncated = scaled.abs().remainder(limit).setScale(scale, RoundingMode.DOWN);
            scaled = scaled.signum() < 0 ? truncated.negate() : truncated;
        }
        return scaled;
    }

    /** Stores a value in a {@code PIC S9(11)V9(4) COMP-3} field. */
    public static BigDecimal quantity(BigDecimal value) {
        return store(value, QUANTITY_DIGITS, QUANTITY_SCALE);
    }

    /** Stores a value in a {@code PIC S9(13)V9(2) COMP-3} field. */
    public static BigDecimal amount(BigDecimal value) {
        return store(value, AMOUNT_DIGITS, AMOUNT_SCALE);
    }

    /** Convenience factory for test data and literals; null stores as zero, as {@link #store} does. */
    public static BigDecimal quantity(String value) {
        return quantity(parse(value));
    }

    /** Convenience factory for test data and literals; null stores as zero, as {@link #store} does. */
    public static BigDecimal amount(String value) {
        return amount(parse(value));
    }

    private static BigDecimal parse(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    /**
     * A printable stand-in for the bytes of a packed field: a sign character followed by
     * {@code digits + scale} digits with the implied decimal point removed. COMP-3 nibbles have no
     * text form, so record images built from packed fields are approximations by construction.
     */
    public static String image(BigDecimal value, int digits, int scale) {
        BigDecimal stored = store(value, digits, scale);
        String unscaled = stored.abs().unscaledValue().toString();
        StringBuilder buffer = new StringBuilder(digits + scale + 1);
        buffer.append(stored.signum() < 0 ? '-' : '+');
        for (int i = unscaled.length(); i < digits + scale; i++) {
            buffer.append('0');
        }
        return buffer.append(unscaled).toString();
    }

    /** {@code IF field <= ZERO} - COBOL compares numerically, ignoring scale. */
    public static boolean isNotPositive(BigDecimal value) {
        return value == null || value.signum() <= 0;
    }
}
