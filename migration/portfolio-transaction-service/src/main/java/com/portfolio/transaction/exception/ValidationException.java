package com.portfolio.transaction.exception;

public class ValidationException extends TransactionException {

    public ValidationException(String message) {
        super("ERR_VALIDATION", message);
    }
}
