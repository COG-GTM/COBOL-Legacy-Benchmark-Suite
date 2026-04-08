package com.portfolio.exception;

/**
 * Base runtime exception for all portfolio-related errors.
 * Replaces the centralized error handling framework from ERRHAND.cpy and ERRHNDL.cbl.
 *
 * In the COBOL system, errors were communicated via the ERR-MESSAGE structure
 * (ERR-PROGRAM, ERR-CATEGORY, ERR-CODE, ERR-SEVERITY, ERR-TEXT, ERR-DETAILS)
 * and processed by the ERRHNDL program. In Java, exceptions propagate naturally
 * up the call stack and are caught by {@code @ControllerAdvice}.
 */
public class PortfolioException extends RuntimeException {

    private final String programId;
    private final String category;
    private final String errorCode;

    public PortfolioException(String message) {
        super(message);
        this.programId = "UNKNOWN";
        this.category = "PR";
        this.errorCode = "9999";
    }

    public PortfolioException(String message, String programId, String category, String errorCode) {
        super(message);
        this.programId = programId;
        this.category = category;
        this.errorCode = errorCode;
    }

    public PortfolioException(String message, Throwable cause) {
        super(message, cause);
        this.programId = "UNKNOWN";
        this.category = "SY";
        this.errorCode = "9999";
    }

    public String getProgramId() {
        return programId;
    }

    public String getCategory() {
        return category;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
