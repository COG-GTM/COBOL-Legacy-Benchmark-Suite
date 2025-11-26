package com.portfolio.transaction.exception;

public class InvalidTransactionTypeException extends TransactionException {

    public InvalidTransactionTypeException(String type) {
        super("ERR_INVALID_TYPE", "Invalid Transaction Type: " + type);
    }
}
