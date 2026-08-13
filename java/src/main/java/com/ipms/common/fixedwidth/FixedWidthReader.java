package com.ipms.common.fixedwidth;

import java.math.BigDecimal;

/**
 * Sequential reader for COBOL-style fixed-width records.
 *
 * <p>Conventions for the Java migration of COBOL {@code PIC} clauses:
 * <ul>
 *   <li>{@code PIC X(n)} — {@link #string(int)}: n characters, trailing spaces stripped.</li>
 *   <li>{@code PIC 9(n)} — {@link #unsignedDecimal(int, int)}: n zoned digits.</li>
 *   <li>{@code PIC S9(p)V9(s)} (including COMP-3/COMP fields, which are stored in
 *       display form in the migrated records) — {@link #signedDecimal(int, int)}:
 *       a leading sign character ('+' or '-') followed by p+s digits with an
 *       implied decimal point; width = p + s + 1.</li>
 * </ul>
 * All decimal values are surfaced as {@link BigDecimal} with the declared scale.
 */
public final class FixedWidthReader {

    private final String record;
    private int position;

    public FixedWidthReader(String record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        this.record = record;
    }

    /** Reads a {@code PIC X(n)} field; trailing spaces are stripped. */
    public String string(int length) {
        String raw = take(length);
        return stripTrailing(raw);
    }

    /** Reads a {@code PIC X(n)} field without stripping trailing spaces. */
    public String rawString(int length) {
        return take(length);
    }

    /** Reads an unsigned zoned decimal ({@code PIC 9(p)V9(s)}); width = precision + scale. */
    public BigDecimal unsignedDecimal(int precision, int scale) {
        String digits = take(precision + scale).trim();
        if (digits.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        return new BigDecimal(digits).movePointLeft(scale).setScale(scale);
    }

    /** Reads a signed decimal ({@code PIC S9(p)V9(s)}); width = 1 (sign) + precision + scale. */
    public BigDecimal signedDecimal(int precision, int scale) {
        String raw = take(1 + precision + scale).trim();
        if (raw.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale);
        }
        boolean negative = raw.charAt(0) == '-';
        String digits = (raw.charAt(0) == '+' || raw.charAt(0) == '-') ? raw.substring(1) : raw;
        BigDecimal value = new BigDecimal(digits).movePointLeft(scale).setScale(scale);
        return negative ? value.negate() : value;
    }

    /** Skips {@code length} characters (e.g. FILLER). */
    public FixedWidthReader skip(int length) {
        take(length);
        return this;
    }

    public int position() {
        return position;
    }

    public int remaining() {
        return record.length() - position;
    }

    private String take(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        if (position + length > record.length()) {
            throw new FixedWidthException(
                    "Record too short: need " + (position + length)
                            + " chars but record has " + record.length());
        }
        String value = record.substring(position, position + length);
        position += length;
        return value;
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}
