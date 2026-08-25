package com.portfolio.batch;

/**
 * Thrown to abort the HISTLD00 job when the error count exceeds 100,
 * matching the COBOL loop condition
 * {@code PERFORM 2000-PROCESS UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100}.
 */
public class ErrorLimitExceededException extends RuntimeException {

    public ErrorLimitExceededException(long errorCount) {
        super("HISTLD00 aborted: error count " + errorCount + " exceeded limit of "
                + HistoryLoadStats.MAX_ERRORS);
    }
}
