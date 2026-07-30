package com.cognition.portfolio.transaction.service;

import com.cognition.portfolio.traceability.CobolOrigin;
import java.util.List;

/**
 * Counters displayed by {@code PORTTRAN 3000-TERMINATE}:
 *
 * <pre>
 * DISPLAY 'Transactions Read:    ' WS-READ-COUNT
 * DISPLAY 'Transactions Process: ' WS-PROCESS-COUNT
 * DISPLAY 'Errors Encountered:   ' WS-ERROR-COUNT
 * </pre>
 *
 * @param readCount {@code WS-READ-COUNT}
 * @param processCount {@code WS-PROCESS-COUNT}
 * @param errorCount {@code WS-ERROR-COUNT}
 * @param abortedOnErrorLimit true when the run stopped because {@code WS-ERROR-COUNT > 100}
 * @param results per-record outcomes, in read order
 */
@CobolOrigin(program = "PORTTRAN", paragraph = "3000-TERMINATE", rules = {"BR-08"})
public record BatchRunSummary(
    int readCount,
    int processCount,
    int errorCount,
    boolean abortedOnErrorLimit,
    List<TransactionProcessingResult> results) {}
