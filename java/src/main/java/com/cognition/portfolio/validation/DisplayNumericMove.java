package com.cognition.portfolio.validation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * {@code MOVE alphanumeric-field TO signed-display-numeric-field}, as implemented by
 * {@code cob_move_alphanum_to_display} in GnuCOBOL 3.1.2 libcob/move.c.
 *
 * <p>Summary of that algorithm, which this class reproduces step for step:
 * <ol>
 *   <li>Skip leading whitespace; accept one leading '+' or '-' as the sign.</li>
 *   <li>Count the digits before the first '.' (anywhere in the rest of the field).</li>
 *   <li>If that count is below the receiver's integer digits, right-align the integer part;
 *       otherwise discard the excess leading digits (and any bytes in between, whatever they
 *       are) - high-order truncation.</li>
 *   <li>Copy digits left to right until the receiver is full; ignore whitespace and ',';
 *       accept one '.'. A second '.' or any other byte zeroes the whole receiver and drops the
 *       sign. Bytes after the receiver is full are never examined; excess decimals are truncated,
 *       not rounded.</li>
 * </ol>
 *
 * <p>GnuCOBOL can leave the receiver holding a negative zero (e.g. from input {@code "-0"} or
 * {@code "-"}); {@link BigDecimal} has no negative zero, so that value is returned as zero. The
 * two are equal in every COBOL numeric comparison.
 *
 * <p>This is <em>not</em> what IBM Enterprise COBOL does for the same statement; see
 * docs/modernization/PORTVALD-migration.md.
 */
final class DisplayNumericMove {

    private static final int DECIMAL_POINT = '.';
    private static final int NUMERIC_SEPARATOR = ',';

    private DisplayNumericMove() {
    }

    /**
     * @param source        content of the sending {@code PIC X(n)} field
     * @param integerDigits integer digits of the receiving {@code PIC S9(i)V9(s)} field
     * @param scale         decimal digits of the receiving field
     * @return the value the receiving field holds after the MOVE, with the given scale
     */
    static BigDecimal alphanumericToSignedDisplay(byte[] source, int integerDigits, int scale) {
        int receiverSize = integerDigits + scale;
        char[] receiver = zeros(receiverSize);

        int s1 = 0;
        int e1 = source.length;
        while (s1 < e1 && AlphanumericField.isSpace(at(source, s1))) {
            s1++;
        }

        int sign = 0;
        if (s1 != e1 && (at(source, s1) == '+' || at(source, s1) == '-')) {
            sign = at(source, s1) == '+' ? 1 : -1;
            s1++;
        }

        int count = 0;
        for (int p = s1; p < e1 && at(source, p) != DECIMAL_POINT; p++) {
            if (AlphanumericField.isDigit(at(source, p))) {
                count++;
            }
        }

        int s2 = 0;
        if (count < integerDigits) {
            s2 = integerDigits - count;
        } else {
            while (count-- > integerDigits) {
                while (!AlphanumericField.isDigit(at(source, s1++))) {
                    // discard bytes up to and including the next digit
                }
            }
        }

        int decimalPoints = 0;
        for (; s1 < e1 && s2 < receiverSize; s1++) {
            int c = at(source, s1);
            if (AlphanumericField.isDigit(c)) {
                receiver[s2++] = (char) c;
            } else if (c == DECIMAL_POINT) {
                if (decimalPoints++ > 0) {
                    return BigDecimal.ZERO.setScale(scale);
                }
            } else if (!(AlphanumericField.isSpace(c) || c == NUMERIC_SEPARATOR)) {
                return BigDecimal.ZERO.setScale(scale);
            }
        }

        BigDecimal value = new BigDecimal(new BigInteger(new String(receiver)), scale);
        return sign < 0 ? value.negate() : value;
    }

    private static int at(byte[] source, int index) {
        return source[index] & 0xFF;
    }

    private static char[] zeros(int size) {
        char[] digits = new char[size];
        Arrays.fill(digits, '0');
        return digits;
    }
}
