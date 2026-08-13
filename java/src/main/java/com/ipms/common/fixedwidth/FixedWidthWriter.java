package com.ipms.common.fixedwidth;

import java.math.BigDecimal;

/**
 * Sequential writer for COBOL-style fixed-width records. Counterpart of
 * {@link FixedWidthReader}; see that class for the field conventions.
 */
public final class FixedWidthWriter {

    private final StringBuilder buffer;
    private final int recordLength;

    public FixedWidthWriter(int recordLength) {
        this.recordLength = recordLength;
        this.buffer = new StringBuilder(recordLength);
    }

    /** Writes a {@code PIC X(n)} field, space padded / truncated to {@code length}. */
    public FixedWidthWriter string(String value, int length) {
        String v = value == null ? "" : value;
        if (v.length() > length) {
            v = v.substring(0, length);
        }
        buffer.append(v);
        pad(length - v.length(), ' ');
        return this;
    }

    /** Writes an unsigned zoned decimal ({@code PIC 9(p)V9(s)}); width = precision + scale. */
    public FixedWidthWriter unsignedDecimal(BigDecimal value, int precision, int scale) {
        buffer.append(digits(value, precision, scale, false));
        return this;
    }

    /** Writes a signed decimal ({@code PIC S9(p)V9(s)}); width = 1 (sign) + precision + scale. */
    public FixedWidthWriter signedDecimal(BigDecimal value, int precision, int scale) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        buffer.append(v.signum() < 0 ? '-' : '+');
        buffer.append(digits(v.abs(), precision, scale, true));
        return this;
    }

    /** Writes {@code length} spaces (e.g. FILLER). */
    public FixedWidthWriter filler(int length) {
        pad(length, ' ');
        return this;
    }

    /** Renders the record, verifying the declared record length was filled exactly. */
    public String toRecord() {
        if (buffer.length() != recordLength) {
            throw new FixedWidthException(
                    "Record length mismatch: expected " + recordLength
                            + " but wrote " + buffer.length());
        }
        return buffer.toString();
    }

    private static String digits(BigDecimal value, int precision, int scale, boolean alreadyAbs) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        if (!alreadyAbs && v.signum() < 0) {
            throw new FixedWidthException("Unsigned field cannot hold negative value " + v);
        }
        String unscaled = v.setScale(scale).unscaledValue().abs().toString();
        int width = precision + scale;
        if (unscaled.length() > width) {
            throw new FixedWidthException(
                    "Value " + v + " exceeds PIC 9(" + precision + ")V9(" + scale + ")");
        }
        return "0".repeat(width - unscaled.length()) + unscaled;
    }

    private void pad(int count, char c) {
        for (int i = 0; i < count; i++) {
            buffer.append(c);
        }
    }
}
