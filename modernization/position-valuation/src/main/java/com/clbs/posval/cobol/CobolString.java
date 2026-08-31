package com.clbs.posval.cobol;

/**
 * {@code PIC X(n)} semantics that the validation rules of this slice depend on.
 *
 * <p>These are not cosmetic. {@code PORTVALD} applies the {@code NUMERIC} class test to whole
 * alphanumeric fields, and an alphanumeric field is always full width: shorter data is padded with
 * spaces, and spaces are not numeric. Reproducing the padding is therefore a prerequisite for
 * reproducing the validator's answers.
 */
public final class CobolString {

    private CobolString() {}

    /** {@code MOVE value TO field PIC X(size)}: left justify, pad right with spaces, truncate right. */
    public static String move(String value, int size) {
        String source = value == null ? "" : value;
        if (source.length() >= size) {
            return source.substring(0, size);
        }
        return source + " ".repeat(size - source.length());
    }

    /**
     * Reference modification {@code field(offset:length)}, 1-based as in COBOL, over a field that
     * has already been padded to its declared width.
     */
    public static String refmod(String field, int offset, int length) {
        return field.substring(offset - 1, offset - 1 + length);
    }

    /**
     * The {@code NUMERIC} class test for an unsigned alphanumeric item: true only when every
     * character position holds a digit. Spaces — including the padding of a short {@code MOVE} —
     * make the test false.
     */
    public static boolean isNumeric(String field) {
        if (field.isEmpty()) {
            return false;
        }
        for (int i = 0; i < field.length(); i++) {
            if (field.charAt(i) < '0' || field.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    /** {@code IF field = ZEROS} for an alphanumeric item: every character position holds {@code '0'}. */
    public static boolean isZeros(String field) {
        if (field.isEmpty()) {
            return false;
        }
        for (int i = 0; i < field.length(); i++) {
            if (field.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    /** {@code IF field = SPACES}: every character position holds a space. */
    public static boolean isSpaces(String field) {
        return field.isBlank();
    }
}
