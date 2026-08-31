package com.clbs.posval.service;

import com.clbs.posval.cobol.CobolDecimal;
import com.clbs.posval.cobol.PackedField;
import com.clbs.posval.cobol.SignedEditedField;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Port of the valuation arithmetic of {@code src/programs/batch/RPTPOS00.cbl}
 * ({@code 2110-FORMAT-POSITION}), the only place in the slice where a portfolio position is valued
 * rather than merely accumulated.
 *
 * <p>The COBOL is a single {@code COMPUTE}:
 *
 * <pre>
 * COMPUTE WS-POS-CHANGE-PCT =
 *     (POS-CURRENT-VALUE - POS-PREVIOUS-VALUE) / POS-PREVIOUS-VALUE * 100
 * </pre>
 *
 * <p>with no {@code ROUNDED} phrase and no {@code ON SIZE ERROR} phrase, which fixes three rules:
 * the result truncates toward zero to two decimals, it wraps modulo 1000 because the receiving
 * field holds three integer digits, and a zero previous value is a size error whose handling is
 * undefined by the standard.
 */
@Service
public class PositionValuationService {

    /**
     * The outcome of {@code 2110-FORMAT-POSITION}'s {@code COMPUTE}.
     *
     * @param sizeError true when the divisor was zero, in which case the report field keeps
     *     whatever it held from the previous position — {@link #percentChange} is null
     * @param percentChange the stored content of {@code WS-POS-CHANGE-PCT}, or null on size error
     * @param overflowed true when the untruncated percentage needed more than three integer digits
     *     and high-order digits were discarded
     */
    public record ChangePercent(boolean sizeError, BigDecimal percentChange, boolean overflowed) {

        /** The seven character rendering of {@code PIC +ZZ9.99}, or null on a size error. */
        public String edited() {
            return sizeError ? null : SignedEditedField.format(percentChange);
        }
    }

    /**
     * {@code 2110-FORMAT-POSITION}: day-over-day percentage change of a position's value.
     *
     * <p>Both operands are truncated to {@code PIC S9(13)V9(2)} before the computation, because
     * that is the width of the position value fields; a current value of 100.005 is therefore
     * indistinguishable from 100.00.
     *
     * <p>When {@code previousValue} is zero the COBOL divides by zero with no {@code ON SIZE
     * ERROR} phrase. Under GnuCOBOL the receiving field is left unchanged, which — because
     * {@code WS-POS-CHANGE-PCT} is never re-initialised between positions — means the report
     * silently repeats the previous position's percentage. This is reproduced, and flagged as spec
     * open question OQ-9: a mainframe run may instead abend with a data exception (S0C7/S0CB),
     * and the two behaviours differ materially for the reader of the report.
     */
    public ChangePercent changePercent(BigDecimal currentValue, BigDecimal previousValue) {
        BigDecimal current = PackedField.AMOUNT.store(currentValue);
        BigDecimal previous = PackedField.AMOUNT.store(previousValue);

        if (previous.signum() == 0) {
            return new ChangePercent(true, null, false);
        }

        BigDecimal raw = CobolDecimal.divide(current.subtract(previous), previous)
                .multiply(BigDecimal.valueOf(100));

        return new ChangePercent(false, SignedEditedField.store(raw), SignedEditedField.PIC.overflows(raw));
    }
}
