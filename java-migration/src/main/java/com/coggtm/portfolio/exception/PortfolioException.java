package com.coggtm.portfolio.exception;

/**
 * Base exception for portfolio operations.
 * Maps to COBOL ERRHAND.cpy ERR-ERROR (return code 8).
 */
public class PortfolioException extends RuntimeException {

    private final int returnCode;

    public PortfolioException(String message) {
        super(message);
        this.returnCode = 8;
    }

    public PortfolioException(String message, Throwable cause) {
        super(message, cause);
        this.returnCode = 8;
    }

    public PortfolioException(String message, int returnCode) {
        super(message);
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
