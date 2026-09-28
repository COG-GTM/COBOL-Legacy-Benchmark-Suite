package com.cognition.portfolio.validation;

import java.util.Arrays;
import java.util.Objects;

/**
 * Fixed-length single-byte alphanumeric ({@code PIC X(n)}) field semantics as implemented by the
 * GnuCOBOL 3.1.2 runtime (libcob) in its default "C" character-type locale.
 *
 * <p>Characters are modelled as unsigned bytes 0x00-0xFF; a Java {@code char} above U+00FF has no
 * single-byte COBOL equivalent and is rejected.
 */
final class AlphanumericField {

    private static final byte SPACE = (byte) ' ';

    private AlphanumericField() {
    }

    /**
     * Builds the byte content of a {@code PIC X(length)} field from {@code value}, right-padding
     * with spaces as an alphanumeric MOVE into the field would.
     *
     * @throws IllegalArgumentException if {@code value} is longer than {@code length} or contains
     *                                  a character above U+00FF
     */
    static byte[] of(String value, int length) {
        Objects.requireNonNull(value, "value");
        if (value.length() > length) {
            throw new IllegalArgumentException(
                    "value has " + value.length() + " characters; field is PIC X(" + length + ")");
        }
        byte[] field = new byte[length];
        Arrays.fill(field, SPACE);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0xFF) {
                throw new IllegalArgumentException(
                        String.format("character U+%04X at index %d is not a single-byte character", (int) c, i));
            }
            field[i] = (byte) c;
        }
        return field;
    }

    static String pad(String value, int length) {
        return toText(of(value, length));
    }

    static String toText(byte[] field) {
        char[] chars = new char[field.length];
        for (int i = 0; i < field.length; i++) {
            chars[i] = (char) (field[i] & 0xFF);
        }
        return new String(chars);
    }

    /** C {@code isdigit} in the "C" locale. */
    static boolean isDigit(int b) {
        return b >= '0' && b <= '9';
    }

    /** C {@code isspace} in the "C" locale: space, TAB, LF, VT, FF, CR. */
    static boolean isSpace(int b) {
        return b == ' ' || (b >= 0x09 && b <= 0x0D);
    }

    /**
     * COBOL class condition {@code IS NUMERIC} on an alphanumeric field: true only if every byte
     * is a digit '0'-'9'. Spaces, signs and decimal points make the field NOT NUMERIC.
     */
    static boolean isNumeric(byte[] field) {
        for (byte b : field) {
            if (!isDigit(b & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    /** {@code field = ZEROS}: every byte is '0'. */
    static boolean isAllZeros(byte[] field) {
        for (byte b : field) {
            if (b != (byte) '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Alphanumeric comparison {@code field = 'literal'}: the shorter operand is treated as if
     * right-padded with spaces, so equality requires the literal followed only by spaces.
     */
    static boolean equalsLiteral(byte[] field, String literal) {
        return Arrays.equals(field, of(literal, field.length));
    }

    /** Reference modification {@code field(start:length)} with a 1-based {@code start}. */
    static byte[] substring(byte[] field, int start, int length) {
        return Arrays.copyOfRange(field, start - 1, start - 1 + length);
    }
}
