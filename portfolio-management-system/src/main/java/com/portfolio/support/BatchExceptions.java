package com.portfolio.support;

/**
 * Custom exception hierarchy for batch processing.
 * Migrated from COBOL ERRPROC return codes:
 *   RC 0  = success (no exception)
 *   RC 4  = warning  -> BatchWarningException
 *   RC 8  = error    -> BatchErrorException
 *   RC 12 = severe   -> BatchSevereException
 *   RC 16 = terminal -> BatchTerminalException
 *
 * From ERRHAND copybook ERR-RETURN-CODES.
 */
public final class BatchExceptions {

    private BatchExceptions() {}

    /** Return code constants matching COBOL ERR-RETURN-CODES */
    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_TERMINAL = 16;

    /**
     * Maps a return code to the appropriate exception.
     * RC 0 returns null (no exception).
     */
    public static RuntimeException fromReturnCode(int returnCode, String message) {
        return switch (returnCode) {
            case RC_SUCCESS -> null;
            case RC_WARNING -> new BatchWarningException(message);
            case RC_ERROR -> new BatchErrorException(message);
            case RC_SEVERE -> new BatchSevereException(message);
            case RC_TERMINAL -> new BatchTerminalException(message);
            default -> new BatchErrorException("Unknown RC " + returnCode + ": " + message);
        };
    }

    /**
     * Returns the return code for a given exception.
     */
    public static int toReturnCode(Exception ex) {
        if (ex instanceof BatchWarningException) return RC_WARNING;
        if (ex instanceof BatchErrorException) return RC_ERROR;
        if (ex instanceof BatchSevereException) return RC_SEVERE;
        if (ex instanceof BatchTerminalException) return RC_TERMINAL;
        return RC_ERROR;
    }

    /** RC 4 - Warning: processing continues but issues noted */
    public static class BatchWarningException extends RuntimeException {
        public BatchWarningException(String message) { super(message); }
        public BatchWarningException(String message, Throwable cause) { super(message, cause); }
        public int getReturnCode() { return RC_WARNING; }
    }

    /** RC 8 - Error: step fails but job may continue depending on gate */
    public static class BatchErrorException extends RuntimeException {
        public BatchErrorException(String message) { super(message); }
        public BatchErrorException(String message, Throwable cause) { super(message, cause); }
        public int getReturnCode() { return RC_ERROR; }
    }

    /** RC 12 - Severe: immediate step termination */
    public static class BatchSevereException extends RuntimeException {
        public BatchSevereException(String message) { super(message); }
        public BatchSevereException(String message, Throwable cause) { super(message, cause); }
        public int getReturnCode() { return RC_SEVERE; }
    }

    /** RC 16 - Terminal: immediate job abort */
    public static class BatchTerminalException extends RuntimeException {
        public BatchTerminalException(String message) { super(message); }
        public BatchTerminalException(String message, Throwable cause) { super(message, cause); }
        public int getReturnCode() { return RC_TERMINAL; }
    }
}
