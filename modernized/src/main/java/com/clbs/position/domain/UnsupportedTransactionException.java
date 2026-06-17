package com.clbs.position.domain;

/**
 * Raised for transaction types the legacy program did not implement. Mirrors
 * {@code PORTTRAN.cbl 2230-PROCESS-TRANSFER}
 * ({@code MOVE 'Transfer processing not implemented' TO ERR-TEXT}). Preserved as
 * an explicit, faithful translation of the COBOL stub rather than inventing
 * undefined transfer semantics.
 */
public class UnsupportedTransactionException extends RuntimeException {
    public UnsupportedTransactionException(String message) {
        super(message);
    }
}
