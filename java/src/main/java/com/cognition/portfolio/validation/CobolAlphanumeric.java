package com.cognition.portfolio.validation;

/**
 * Helpers that reproduce the storage and comparison semantics of COBOL
 * alphanumeric ({@code PIC X(n)}) items.
 *
 * <p>Java strings are variable length; COBOL {@code PIC X(n)} items are not.
 * Every value that crosses the PORTVALD interface lives in a fixed length
 * byte field, and several of the program's rules only make sense once the
 * padding is taken into account, so the padding is modelled explicitly here
 * rather than being hidden inside the validator.</p>
 */
public final class CobolAlphanumeric {

    private CobolAlphanumeric() {
    }

    /**
     * Reproduces {@code MOVE source TO target} where {@code target} is
     * {@code PIC X(length)}: the value is left justified, padded on the right
     * with spaces, and truncated on the right when it is too long.
     *
     * @param source the sending value; {@code null} is treated as all spaces
     * @param length the length of the receiving field in bytes
     */
    public static String toFixedLength(String source, int length) {
        String value = source == null ? "" : source;
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }

    /**
     * Reproduces the COBOL class condition {@code IS NUMERIC} for an
     * alphanumeric item: the item is numeric only when every byte is a digit.
     * A trailing pad space makes the item non numeric, which is the behaviour
     * PORTVALD depends on in 1000-VALIDATE-ID and 2000-VALIDATE-ACCOUNT.
     */
    public static boolean isNumeric(String field) {
        if (field == null || field.isEmpty()) {
            return false;
        }
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Reproduces {@code IF field = ZEROS} for an alphanumeric item: the
     * figurative constant is expanded to the length of the field, so the
     * comparison is true only when every byte is the character zero.
     */
    public static boolean equalsZeros(String field) {
        if (field == null || field.isEmpty()) {
            return false;
        }
        for (int i = 0; i < field.length(); i++) {
            if (field.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Reproduces reference modification {@code field(offset:length)} using
     * one based COBOL offsets.
     */
    public static String refMod(String field, int offset, int length) {
        return field.substring(offset - 1, offset - 1 + length);
    }
}
