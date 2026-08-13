package com.ipms.common.db;

/** Raised by database connection/transaction components; carries the COBOL-style return code. */
public class ConnectionException extends RuntimeException {

    private final int returnCode;

    public ConnectionException(String message, int returnCode, Throwable cause) {
        super(message, cause);
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
