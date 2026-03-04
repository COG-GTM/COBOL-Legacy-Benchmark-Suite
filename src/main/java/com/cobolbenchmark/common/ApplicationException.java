package com.cobolbenchmark.common;

/**
 * Base exception for the Portfolio Management application.
 * Replaces COBOL PERFORM 9000-ERROR-ROUTINE patterns.
 */
public class ApplicationException extends RuntimeException {

    private final String errorCode;
    private final String programId;

    public ApplicationException(String message) {
        super(message);
        this.errorCode = "UNKNOWN";
        this.programId = "UNKNOWN";
    }

    public ApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.programId = "UNKNOWN";
    }

    public ApplicationException(String errorCode, String programId, String message) {
        super(message);
        this.errorCode = errorCode;
        this.programId = programId;
    }

    public ApplicationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.programId = "UNKNOWN";
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getProgramId() {
        return programId;
    }
}
