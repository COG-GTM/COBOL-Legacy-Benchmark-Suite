package com.portfolio.exception;

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
