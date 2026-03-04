package com.cobolbenchmark.common;

/**
 * Exception for record not found conditions.
 * Replaces COBOL VSAM NOTFND status and DB2 SQLCODE +100.
 */
public class RecordNotFoundException extends ApplicationException {

    public RecordNotFoundException(String message) {
        super("NOTFND", message);
    }

    public RecordNotFoundException(String entityType, String key) {
        super("NOTFND", entityType + " not found with key: " + key);
    }
}
