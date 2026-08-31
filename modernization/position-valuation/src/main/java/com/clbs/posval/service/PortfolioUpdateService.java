package com.clbs.posval.service;

import com.clbs.posval.cobol.CobolString;
import com.clbs.posval.cobol.PackedField;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Port of {@code src/programs/portfolio/PORTUPDT.cbl} — the batch that applies a sequential file of
 * field-level amendments to the portfolio master.
 *
 * <table border="1">
 *   <caption>PORTUPDT to Java</caption>
 *   <tr><th>COBOL paragraph</th><th>Java method</th></tr>
 *   <tr><td>{@code 2100-PROCESS-UPDATE}</td><td>{@link #applyUpdate}</td></tr>
 *   <tr><td>{@code 2200-APPLY-UPDATE}</td><td>{@link #convertValue}</td></tr>
 * </table>
 *
 * <p>Only the {@code 'V'} action touches money, and it does so through an alphanumeric-to-numeric
 * {@code MOVE}, which is where this program's one non-obvious rule lives.
 */
@Service
public class PortfolioUpdateService {

    /** {@code UPDT-STATUS VALUE 'S'} — replace {@code PORT-STATUS}. */
    public static final char ACTION_STATUS = 'S';
    /** {@code UPDT-VALUE VALUE 'V'} — replace {@code PORT-TOTAL-VALUE}. */
    public static final char ACTION_VALUE = 'V';
    /** {@code UPDT-NAME VALUE 'N'} — replace {@code PORT-CLIENT-NAME}. */
    public static final char ACTION_NAME = 'N';

    /** {@code UPDT-NEW-VALUE PIC X(50)}. */
    public static final int NEW_VALUE_WIDTH = 50;

    /**
     * The result of applying one update record.
     *
     * @param applied false when the keyed read failed, i.e. the COBOL counted the record as an
     *     error and displayed {@code 'Record not found: '}
     * @param status new {@code PORT-STATUS}, when the action was {@code 'S'}
     * @param clientName new {@code PORT-CLIENT-NAME}, when the action was {@code 'N'}
     * @param totalValue new {@code PORT-TOTAL-VALUE}, when the action was {@code 'V'}
     */
    public record UpdateOutcome(
            boolean applied, String status, String clientName, BigDecimal totalValue) {}

    /**
     * {@code 2100-PROCESS-UPDATE} / {@code 2200-APPLY-UPDATE}: reads the portfolio by
     * {@code UPDT-KEY} and applies the one field named by {@code UPDT-ACTION}.
     *
     * <p>An action outside {@code S}, {@code V} and {@code N} falls through the {@code EVALUATE}
     * with no {@code WHEN OTHER}: the record is rewritten unchanged and counted as a successful
     * update.
     *
     * <p>{@code 'S'} moves a {@code PIC X(50)} field into {@code PORT-STATUS PIC X(1)}, keeping
     * only the first character, and no validation is applied — {@code PORTUPDT} can set a status
     * that {@code PORTMSTR} would reject (spec open question OQ-10).
     */
    public UpdateOutcome applyUpdate(boolean recordFound, char action, String newValue) {
        if (!recordFound) {
            return new UpdateOutcome(false, null, null, null);
        }

        String value = CobolString.move(newValue, NEW_VALUE_WIDTH);

        return switch (action) {
            case ACTION_STATUS -> new UpdateOutcome(true, CobolString.move(value, 1), null, null);
            case ACTION_NAME -> new UpdateOutcome(true, null, CobolString.move(value, 30), null);
            case ACTION_VALUE -> new UpdateOutcome(true, null, null, convertValue(value));
            default -> new UpdateOutcome(true, null, null, null);
        };
    }

    /**
     * The {@code 'V'} branch of {@code 2200-APPLY-UPDATE}:
     * {@code MOVE UPDT-NEW-VALUE TO WS-NUMERIC-WORK} followed by
     * {@code MOVE WS-NUMERIC-WORK TO PORT-TOTAL-VALUE}.
     *
     * <p>{@code WS-NUMERIC-WORK} is {@code PIC S9(13)V99} and the source is {@code PIC X(50)}, and
     * an alphanumeric-to-numeric {@code MOVE} is the one construct in this slice whose result is
     * <b>not portable</b>. Measured under GnuCOBOL (see {@code parity/cobol/PUPDMOV.cbl}), leading
     * and trailing spaces are ignored, an embedded decimal point is honoured, and the value is
     * then truncated to the receiving field: {@code "12500.00"} yields 12,500.00 and
     * {@code "999999999999999"} yields 9,999,999,999,999.00. IBM Enterprise COBOL specifies the
     * sending item be treated as an unsigned integer aligned at the rightmost digit position, with
     * no decimal point handling, which for {@code "12500.00"} gives a different number.
     * Amendment files that carry a decimal point therefore mean different money on the two
     * platforms; this is spec rule R-11.3 and open question OQ-11.
     */
    public BigDecimal convertValue(String newValue) {
        String field = CobolString.move(newValue, NEW_VALUE_WIDTH).trim();

        if (!field.matches("[+-]?[0-9]*(\\.[0-9]*)?") || field.replaceAll("[^0-9]", "").isEmpty()) {
            throw new IllegalArgumentException(
                    "non-numeric amendment value is implementation-defined in COBOL: " + newValue);
        }

        return PackedField.AMOUNT.store(new BigDecimal(field));
    }
}
