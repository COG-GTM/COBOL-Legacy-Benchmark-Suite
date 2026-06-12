package com.clbs.common.cobol;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helpers for translating between Java values and the fixed-width DISPLAY
 * representation used by the COBOL copybook record layouts.
 *
 * <p>Numeric (COMP-3 / PIC 9) fields are rendered in a zoned, zero-padded
 * DISPLAY form (implied decimal point, no separator) so that golden fixtures
 * are deterministic and round-trippable. Packed-decimal bytes are intentionally
 * not reproduced because they cannot be regenerated without the original COBOL
 * runtime; see {@code java/docs/field-mappings.md}.
 */
public final class CobolField {

    private CobolField() {
    }

    /** PIC X(len): left-justified, space-padded, truncated to {@code len}. */
    public static String alphanumeric(String value, int len) {
        String v = value == null ? "" : value;
        if (v.length() > len) {
            return v.substring(0, len);
        }
        StringBuilder sb = new StringBuilder(v);
        while (sb.length() < len) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** Parse a PIC X(len) field, trimming trailing spaces. */
    public static String parseAlphanumeric(String raw) {
        if (raw == null) {
            return "";
        }
        int end = raw.length();
        while (end > 0 && raw.charAt(end - 1) == ' ') {
            end--;
        }
        return raw.substring(0, end);
    }

    /** PIC 9(len): unsigned integer, right-justified, zero-padded. */
    public static String integer(long value, int len) {
        String digits = Long.toString(Math.abs(value));
        if (digits.length() > len) {
            throw new IllegalArgumentException("value " + value + " exceeds PIC 9(" + len + ")");
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < len - digits.length()) {
            sb.append('0');
        }
        return sb.append(digits).toString();
    }

    /** Parse a PIC 9(len) field. */
    public static long parseInteger(String raw) {
        return Long.parseLong(raw.trim());
    }

    /**
     * Render a COMP-3 numeric value, e.g. {@code S9(13)V99}, as zoned DISPLAY
     * digits with an implied decimal point (no separator character).
     *
     * @param value     the decimal value (assumed non-negative for fixtures)
     * @param intDigits number of digits before the implied decimal
     * @param decDigits number of digits after the implied decimal
     */
    public static String numeric(BigDecimal value, int intDigits, int decDigits) {
        BigDecimal scaled = value.abs().setScale(decDigits, RoundingMode.HALF_UP);
        String unscaled = scaled.unscaledValue().toString();
        int totalDigits = intDigits + decDigits;
        if (unscaled.length() > totalDigits) {
            throw new IllegalArgumentException(
                    "value " + value + " exceeds S9(" + intDigits + ")V9(" + decDigits + ")");
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < totalDigits - unscaled.length()) {
            sb.append('0');
        }
        return sb.append(unscaled).toString();
    }

    /** Parse a zoned DISPLAY numeric field back into a scaled {@link BigDecimal}. */
    public static BigDecimal parseNumeric(String raw, int decDigits) {
        BigDecimal unscaled = new BigDecimal(raw.trim());
        return unscaled.movePointLeft(decDigits).setScale(decDigits, RoundingMode.UNNECESSARY);
    }
}
