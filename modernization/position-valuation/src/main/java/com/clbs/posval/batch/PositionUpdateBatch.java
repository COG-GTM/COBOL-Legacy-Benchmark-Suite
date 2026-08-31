package com.clbs.posval.batch;

import com.clbs.posval.domain.TransactionRecord;
import com.clbs.posval.error.ErrorProcessor;
import com.clbs.posval.error.ErrorRecord;
import com.clbs.posval.service.PositionUpdateService;
import com.clbs.posval.service.TransactionValidationService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Port of the driver of {@code src/programs/portfolio/PORTTRAN.cbl}
 * ({@code 0000-MAIN}, {@code 2000-PROCESS-TRANSACTIONS}, {@code 3000-TERMINATE},
 * {@code 9000-ERROR-ROUTINE}), which is also the batch step
 * {@code src/programs/batch/POSUPDT.cbl} is documented to run.
 *
 * <p><b>The single most consequential finding of this migration.</b> {@code PORTTRAN} never
 * performs {@code 2200-UPDATE-POSITIONS}. {@code 2000-PROCESS-TRANSACTIONS} reads a transaction
 * and performs {@code 2100-VALIDATE-TRANSACTION}, which either increments
 * {@code WS-PROCESS-COUNT} or logs an error — and then the loop reads the next record. No
 * {@code PERFORM 2200-UPDATE-POSITIONS} statement exists anywhere in the program, so the buy,
 * sell, transfer, fee and audit paragraphs are unreachable: as written, the program validates a
 * transaction file and reports counts, and no portfolio balance ever changes.
 *
 * <p>Both behaviours are therefore available here and the choice is explicit rather than
 * accidental:
 *
 * <ul>
 *   <li>{@code clbs.porttran.apply-updates=false} (the default) reproduces the COBOL exactly:
 *       validate only.
 *   <li>{@code clbs.porttran.apply-updates=true} additionally performs the dead paragraphs, which
 *       is what the program's name, its {@code I-O} open of the portfolio file, and the system
 *       architecture document all say the step is supposed to do.
 * </ul>
 *
 * <p>This is spec open question OQ-2 and needs a human answer before anything is cut over.
 */
@Service
public class PositionUpdateBatch {

    /** {@code UNTIL … OR WS-ERROR-COUNT > 100} in {@code 0000-MAIN}. */
    public static final int ERROR_LIMIT = 100;

    private final TransactionValidationService validationService;
    private final PositionUpdateService updateService;
    private final ErrorProcessor errorProcessor;
    private final boolean applyUpdates;

    public PositionUpdateBatch(
            TransactionValidationService validationService,
            PositionUpdateService updateService,
            ErrorProcessor errorProcessor,
            @Value("${clbs.porttran.apply-updates:false}") boolean applyUpdates) {
        this.validationService = validationService;
        this.updateService = updateService;
        this.errorProcessor = errorProcessor;
        this.applyUpdates = applyUpdates;
    }

    /**
     * The counters displayed by {@code 3000-TERMINATE}.
     *
     * @param read {@code WS-READ-COUNT}
     * @param processed {@code WS-PROCESS-COUNT} — transactions that passed validation
     * @param errors {@code WS-ERROR-COUNT}
     * @param haltedOnErrorLimit true when the loop stopped because more than 100 errors were seen,
     *     leaving the rest of the transaction file unread
     */
    public record BatchResult(int read, int processed, int errors, boolean haltedOnErrorLimit) {}

    /** {@code 0000-MAIN} / {@code 2000-PROCESS-TRANSACTIONS}. */
    public BatchResult run(List<TransactionRecord> transactions) {
        int read = 0;
        int processed = 0;
        int errors = 0;

        for (TransactionRecord transaction : transactions) {
            if (errors > ERROR_LIMIT) {
                return new BatchResult(read, processed, errors, true);
            }
            read++;

            Optional<String> validationError = validationService.validate(transaction);
            if (validationError.isPresent()) {
                errors += raise(validationError.get());
                continue;
            }
            processed++;

            if (applyUpdates) {
                Optional<String> updateError = updateService.apply(transaction);
                if (updateError.isPresent()) {
                    errors += raise(updateError.get());
                }
            }
        }

        return new BatchResult(read, processed, errors, false);
    }

    /** {@code 9000-ERROR-ROUTINE}: count the error and hand it to {@code ERRPROC}. */
    private int raise(String text) {
        errorProcessor.process(ErrorRecord.processing(PositionUpdateService.PROGRAM, text));
        return 1;
    }
}
