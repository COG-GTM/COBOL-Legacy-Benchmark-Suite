package com.portfolio.portmstr.exception;

/**
 * Thrown during batch processing failures.
 * Equivalent to COBOL batch job ABEND or severe error conditions.
 */
public class BatchProcessingException extends RuntimeException {

    private final int returnCode;

    public BatchProcessingException(String message, int returnCode) {
        super(message);
        this.returnCode = returnCode;
    }

    public BatchProcessingException(String message, int returnCode, Throwable cause) {
        super(message, cause);
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
