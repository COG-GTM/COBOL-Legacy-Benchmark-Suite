package com.portfolio.transaction.exception;

public abstract class TransactionException extends RuntimeException {

    private final String errorCode;

    protected TransactionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
