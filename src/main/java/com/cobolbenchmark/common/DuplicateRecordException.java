package com.cobolbenchmark.common;

/**
 * Exception for duplicate record conditions.
 * Replaces COBOL VSAM DUPKEY status and DB2 SQLCODE -803.
 */
public class DuplicateRecordException extends ApplicationException {

    public DuplicateRecordException(String message) {
        super("DUPKEY", message);
    }

    public DuplicateRecordException(String entityType, String key) {
        super("DUPKEY", "Duplicate " + entityType + " with key: " + key);
    }
}
