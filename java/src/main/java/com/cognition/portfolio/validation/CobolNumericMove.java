package com.cognition.portfolio.validation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Reproduces {@code MOVE alphanumeric-item TO signed-numeric-display-item},
 * the conversion PORTVALD performs in 4000-VALIDATE-AMOUNT when it moves
 * {@code LS-INPUT-VALUE PIC X(50)} into {@code VAL-TEMP-NUM PIC S9(13)V99}.
 *
 * <p>The behaviour implemented here is the behaviour of GnuCOBOL 3.1.2, the
 * runtime the parity harness executes against. It was established by running
 * the compiled module and probe programs, not by reading the standard: the
 * standard leaves an alphanumeric-to-numeric move of non-numeric data
 * undefined, and IBM Enterprise COBOL does not parse a sign, a decimal point
 * or separators here at all. See "Known gaps" in
 * docs/modernization/PORTVALD-migration.md.</p>
 *
 * <p>The observed algorithm, over the bytes of the sending field:</p>
 * <ol>
 *   <li>leading whitespace is skipped;</li>
 *   <li>a single leading {@code +} or {@code -} sets the sign;</li>
 *   <li>the digits appearing before the first {@code .} are counted. If there
 *       are more of them than the receiving field has integer positions, the
 *       scan is advanced past the surplus high order digits (and past any
 *       non-digit bytes mixed in among them, which is why junk in the skipped
 *       prefix never raises an error). Otherwise the first digit is stored
 *       right aligned against the implied decimal point;</li>
 *   <li>the remaining bytes are then consumed until the receiving field is
 *       full: digits are stored, whitespace and {@code ,} are ignored, the
 *       first {@code .} is ignored, and any other byte - including a second
 *       {@code .} or a trailing sign - makes the whole move fail;</li>
 *   <li>a failed move stores zero, it does not store the digits seen so far;</li>
 *   <li>once the receiving field is full the scan stops, so bytes beyond that
 *       point are never examined and cannot fail the move;</li>
 *   <li>surplus fraction digits are truncated, never rounded.</li>
 * </ol>
 *
 * <p>Worked examples, all with {@code S9(13)V99} as the receiving item:
 * {@code "AB123456789012345"} yields {@code 3456789012345.00} (two surplus
 * integer digits skipped, taking "AB12" with them), {@code "AB12345X"} yields
 * {@code 0.00} (no surplus, so the {@code X} is reached), and
 * {@code "1.234"} yields {@code 1.23}.</p>
 */
public final class CobolNumericMove {

    private CobolNumericMove() {
    }

    /**
     * @param source         the sending alphanumeric field, already padded to
     *                       its declared length
     * @param integerDigits  number of digit positions before the implied
     *                       decimal point (13 for {@code S9(13)V99})
     * @param fractionDigits number of digit positions after it (2)
     * @return the value stored in the receiving item, as a BigDecimal with
     *         scale {@code fractionDigits}
     */
    public static BigDecimal toSignedDisplay(String source, int integerDigits, int fractionDigits) {
        int end = source.length();
        int cursor = 0;

        while (cursor < end && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }

        boolean negative = false;
        if (cursor < end && (source.charAt(cursor) == '+' || source.charAt(cursor) == '-')) {
            negative = source.charAt(cursor) == '-';
            cursor++;
        }

        int integerDigitsPresent = 0;
        for (int i = cursor; i < end && source.charAt(i) != '.'; i++) {
            if (isDigit(source.charAt(i))) {
                integerDigitsPresent++;
            }
        }

        char[] stored = new char[integerDigits + fractionDigits];
        Arrays.fill(stored, '0');
        int position;
        if (integerDigitsPresent < integerDigits) {
            position = integerDigits - integerDigitsPresent;
        } else {
            position = 0;
            cursor = skipDigits(source, cursor, integerDigitsPresent - integerDigits);
        }

        boolean decimalPointSeen = false;
        for (; cursor < end && position < stored.length; cursor++) {
            char c = source.charAt(cursor);
            if (isDigit(c)) {
                stored[position++] = c;
            } else if (c == '.') {
                if (decimalPointSeen) {
                    return zero(fractionDigits);
                }
                decimalPointSeen = true;
            } else if (c != ',' && !Character.isWhitespace(c)) {
                return zero(fractionDigits);
            }
        }

        BigDecimal value = new BigDecimal(new BigInteger(new String(stored)), fractionDigits);
        return negative ? value.negate() : value;
    }

    /** Advances past {@code count} digits, consuming whatever lies between them. */
    private static int skipDigits(String source, int from, int count) {
        int cursor = from;
        int remaining = count;
        while (remaining > 0 && cursor < source.length()) {
            if (isDigit(source.charAt(cursor))) {
                remaining--;
            }
            cursor++;
        }
        return cursor;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static BigDecimal zero(int fractionDigits) {
        return BigDecimal.ZERO.setScale(fractionDigits, RoundingMode.UNNECESSARY);
    }
}
