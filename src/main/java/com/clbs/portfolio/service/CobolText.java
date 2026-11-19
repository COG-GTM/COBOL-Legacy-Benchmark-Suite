package com.clbs.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Helpers that reproduce COBOL PIC X / NUMERIC semantics on Java strings. */
public final class CobolText {

    private CobolText() {
    }

    /** MOVE to a PIC X(n) field: truncate or pad with spaces on the right. */
    public static String pic(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() >= length ? v.substring(0, length) : v + " ".repeat(length - v.length());
    }

    /** {@code IF field = SPACES}. */
    public static boolean isSpaces(String value) {
        return value == null || value.isBlank();
    }

    /** Class test {@code IS NUMERIC} on an alphanumeric item: every character is a digit. */
    public static boolean isNumeric(String value) {
        return !value.isEmpty() && value.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    /** Value as stored in a PIC X field and read back (trailing spaces are not significant). */
    public static String trimRight(String value) {
        return value == null ? "" : value.stripTrailing();
    }

    /**
     * MOVE of an alphanumeric item to {@code PIC S9(13)V99}: a decimal number, truncated to two decimals and to
     * thirteen integer digits. Returns {@code null} when the text is not a number.
     */
    public static BigDecimal toAmount(String value) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        try {
            BigDecimal d = new BigDecimal(v).setScale(2, RoundingMode.DOWN);
            return d.remainder(BigDecimal.TEN.pow(13));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
