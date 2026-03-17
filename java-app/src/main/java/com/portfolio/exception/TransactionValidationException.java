package com.portfolio.exception;

/**
 * Exception thrown when transaction validation fails.
 * Replaces: TRNVAL00.cbl validation error handling.
 */
public class TransactionValidationException extends RuntimeException {

    public TransactionValidationException(String message) {
        super(message);
    }

    public TransactionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
