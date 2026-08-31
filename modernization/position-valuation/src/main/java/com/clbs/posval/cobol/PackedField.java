package com.clbs.posval.cobol;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A COBOL numeric field description, i.e. {@code PIC S9(intDigits)V9(scale)}, held either as
 * {@code COMP-3} (packed decimal) or as unpacked display. Storage class does not change the
 * arithmetic result, only the byte layout, so a single type covers both.
 *
 * <p>Two properties of COBOL numeric assignment must survive the migration:
 *
 * <ol>
 *   <li><b>Low-order truncation.</b> Storing a value with more decimal places than the receiving
 *       field keeps {@code scale} digits and discards the rest. COBOL truncates toward zero; it
 *       does not round unless the statement carries the {@code ROUNDED} phrase. No statement in
 *       this slice carries {@code ROUNDED}, so every store uses {@link RoundingMode#DOWN}.
 *   <li><b>High-order truncation.</b> Storing a value whose integer part exceeds {@code intDigits}
 *       digits silently discards the excess high-order digits when the statement carries no
 *       {@code ON SIZE ERROR} phrase. No statement in this slice carries {@code ON SIZE ERROR},
 *       so overflow wraps modulo {@code 10^intDigits}, keeping the sign of the untruncated value.
 * </ol>
 *
 * @param name COBOL field name, for diagnostics
 * @param intDigits number of digit positions to the left of the implied decimal point
 * @param scale number of digit positions to the right of the implied decimal point
 */
public record PackedField(String name, int intDigits, int scale) {

    /** {@code POS-QUANTITY} / {@code TRN-QUANTITY} — {@code PIC S9(11)V9(4) COMP-3} (POSREC, TRNREC). */
    public static final PackedField QUANTITY = new PackedField("S9(11)V9(4)", 11, 4);

    /**
     * {@code POS-COST-BASIS} / {@code POS-MARKET-VALUE} / {@code TRN-AMOUNT} /
     * {@code PORT-TOTAL-VALUE} — {@code PIC S9(13)V9(2) COMP-3} (POSREC, TRNREC, PORTFLIO).
     */
    public static final PackedField AMOUNT = new PackedField("S9(13)V9(2)", 13, 2);

    /** {@code VAL-TEMP-NUM} — {@code PIC S9(13)V99} display (PORTVAL copybook). */
    public static final PackedField VALIDATION_AMOUNT = new PackedField("S9(13)V99", 13, 2);

    public PackedField {
        if (intDigits <= 0 || scale < 0) {
            throw new IllegalArgumentException("invalid PIC: " + name);
        }
    }

    /**
     * Applies COBOL assignment semantics: truncate toward zero to {@link #scale} decimal places,
     * then discard integer digits beyond {@link #intDigits}.
     */
    public BigDecimal store(BigDecimal value) {
        BigDecimal scaled = value.setScale(scale, RoundingMode.DOWN);
        BigDecimal modulus = BigDecimal.TEN.pow(intDigits);
        // BigDecimal.remainder keeps the sign of the dividend, which is what packed-decimal
        // high-order truncation does: the sign nibble is untouched by the lost digits.
        return scaled.abs().compareTo(modulus) >= 0 ? scaled.remainder(modulus) : scaled;
    }

    /** True when {@code value} does not fit in this field and {@link #store} would lose digits. */
    public boolean overflows(BigDecimal value) {
        return value.setScale(scale, RoundingMode.DOWN).abs().compareTo(BigDecimal.TEN.pow(intDigits)) >= 0;
    }

    /** The zero value at this field's scale, i.e. the content of the field after {@code INITIALIZE}. */
    public BigDecimal zero() {
        return BigDecimal.ZERO.setScale(scale);
    }
}
