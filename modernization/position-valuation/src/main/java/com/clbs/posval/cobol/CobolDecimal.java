package com.clbs.posval.cobol;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * The COBOL {@code ADD} / {@code SUBTRACT} / {@code COMPUTE} verbs of the position valuation and
 * update slice, expressed over {@link BigDecimal}.
 *
 * <p>Every operation states its receiving field explicitly, because in COBOL the receiving field's
 * {@code PIC} clause — not the operands — decides the scale and the rounding of the result.
 * Intermediate results of a {@code COMPUTE} are held in an implementation-defined high-precision
 * intermediate; {@link #INTERMEDIATE} models that with 34 significant digits, and truncation to
 * the receiving field happens once, at the end, exactly as the standard requires.
 */
public final class CobolDecimal {

    /**
     * Precision of COBOL arithmetic intermediates. IBM Enterprise COBOL carries at least 30
     * significant digits for fixed-point intermediates; 34 (IEEE decimal128) is at least as wide,
     * and every division in this slice is followed by truncation to two decimal places, so any
     * width beyond ~20 digits yields identical results.
     */
    public static final MathContext INTERMEDIATE = MathContext.DECIMAL128;

    private CobolDecimal() {}

    /** {@code ADD b TO a} where {@code a} is a field of {@code field}'s PIC clause. */
    public static BigDecimal add(BigDecimal a, BigDecimal b, PackedField field) {
        return field.store(field.store(a).add(field.store(b)));
    }

    /** {@code SUBTRACT b FROM a} where {@code a} is a field of {@code field}'s PIC clause. */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b, PackedField field) {
        return field.store(field.store(a).subtract(field.store(b)));
    }

    /** Divides at intermediate precision; the caller truncates to the receiving field. */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return a.divide(b, INTERMEDIATE);
    }
}
