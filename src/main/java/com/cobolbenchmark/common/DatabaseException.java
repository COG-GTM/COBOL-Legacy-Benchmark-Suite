package com.cobolbenchmark.common;

/**
 * Exception for database-related errors.
 * Replaces COBOL DB2-ERROR-ROUTINE and SQLCODE error handling.
 */
public class DatabaseException extends ApplicationException {

    private final int sqlCode;

    public DatabaseException(String message) {
        super("DBERR", message);
        this.sqlCode = -1;
    }

    public DatabaseException(String message, Throwable cause) {
        super("DBERR", message, cause);
        this.sqlCode = -1;
    }

    public DatabaseException(int sqlCode, String message) {
        super("DBERR", "SQLCODE=" + sqlCode + ": " + message);
        this.sqlCode = sqlCode;
    }

    public int getSqlCode() {
        return sqlCode;
    }
}
