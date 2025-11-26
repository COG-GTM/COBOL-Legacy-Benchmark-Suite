package com.portfolio.transaction.exception;

public class InsufficientUnitsException extends TransactionException {

    public InsufficientUnitsException(String message) {
        super("ERR_INSUFFICIENT_UNITS", message);
    }
}
