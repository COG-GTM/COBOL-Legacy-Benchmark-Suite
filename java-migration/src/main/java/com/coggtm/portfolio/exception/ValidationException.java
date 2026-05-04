package com.coggtm.portfolio.exception;

/**
 * Validation failure exception.
 * Maps to COBOL ERRHAND.cpy ERR-WARNING (return code 4).
 */
public class ValidationException extends PortfolioException {

    public ValidationException(String message) {
        super(message, 4);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
