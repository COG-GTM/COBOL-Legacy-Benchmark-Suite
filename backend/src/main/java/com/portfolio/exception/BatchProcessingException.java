package com.portfolio.exception;

/**
 * Replaces COBOL batch error conditions (RC > 4).
 */
public class BatchProcessingException extends RuntimeException {

    private final int returnCode;

    public BatchProcessingException(String message, int returnCode) {
        super(message);
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
