package com.coggtm.portfolio.exception;

/**
 * Severe / unrecoverable error.
 * Maps to COBOL ERRHAND.cpy ERR-SEVERE (return code 12).
 */
public class SevereException extends PortfolioException {

    public SevereException(String message) {
        super(message, 12);
    }

    public SevereException(String message, Throwable cause) {
        super(message, cause, 12);
    }
}
