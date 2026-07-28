package com.clbs.portfolio.model;

/**
 * Helpers reproducing COBOL alphanumeric ({@code PIC X(n)}) storage semantics.
 *
 * <p>A {@code MOVE} into a {@code PIC X(n)} field left-justifies the source, pads on the right with
 * spaces and truncates anything beyond {@code n} characters. Fields are therefore never null and
 * comparisons such as {@code IF ERR-TEXT = SPACES} test a blank buffer, not an empty string. Every
 * alphanumeric field in the translated model stores its padded form so that string equality in Java
 * matches record equality on the mainframe.
 */
public final class CobolText {

    private CobolText() {
    }

    /** Stores {@code value} as it would be held in a {@code PIC X(length)} field. */
    public static String picX(String value, int length) {
        if (value == null) {
            return spaces(length);
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        StringBuilder padded = new StringBuilder(length).append(value);
        while (padded.length() < length) {
            padded.append(' ');
        }
        return padded.toString();
    }

    /** A buffer of {@code length} spaces, the value of an initialised alphanumeric field. */
    public static String spaces(int length) {
        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            buffer.append(' ');
        }
        return buffer.toString();
    }

    /** {@code IF field = SPACES}. */
    public static boolean isSpaces(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** The field content without its trailing pad, for logging and assertions. */
    public static String trim(String value) {
        if (value == null) {
            return "";
        }
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            end--;
        }
        return value.substring(0, end);
    }

    /**
     * Stores {@code value} as it would be held in an unsigned {@code PIC 9(digits)} display field:
     * the sign is dropped and high-order digits beyond {@code digits} are truncated.
     */
    public static int pic9(long value, int digits) {
        long modulus = (long) Math.pow(10, digits);
        return (int) (Math.abs(value) % modulus);
    }

    /** The stored characters of an unsigned {@code PIC 9(digits)} display field, zero-padded. */
    public static String pic9Image(int value, int digits) {
        return String.format("%0" + digits + "d", pic9(value, digits));
    }
}
