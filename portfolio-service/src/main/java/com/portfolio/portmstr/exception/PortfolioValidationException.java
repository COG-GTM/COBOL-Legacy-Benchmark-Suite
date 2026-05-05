package com.portfolio.portmstr.exception;

/**
 * Thrown when portfolio data fails validation.
 * Equivalent to COBOL PORTVALD.cbl validation failures.
 */
public class PortfolioValidationException extends RuntimeException {

    private final int validationCode;

    public PortfolioValidationException(String message, int validationCode) {
        super(message);
        this.validationCode = validationCode;
    }

    public int getValidationCode() {
        return validationCode;
    }
}
