package com.clbs.position.domain;

/**
 * Raised when a transaction fails the field-level edits performed by the COBOL
 * validation paragraphs ({@code PORTTRAN.cbl 2120-CHECK-TRANSACTION-TYPE} /
 * {@code 2130-CHECK-AMOUNTS}) and the data-dictionary validation rules (5.1).
 */
public class TransactionValidationException extends RuntimeException {
    public TransactionValidationException(String message) {
        super(message);
    }
}
