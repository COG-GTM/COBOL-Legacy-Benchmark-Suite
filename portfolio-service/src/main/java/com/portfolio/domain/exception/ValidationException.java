package com.portfolio.domain.exception;

/**
 * Thrown when a transaction or portfolio operation fails validation.
 * Maps the validation return codes from PORTVAL.cpy.
 */
public class ValidationException extends RuntimeException {

    private final int validationCode;

    public ValidationException(int validationCode, String message) {
        super(message);
        this.validationCode = validationCode;
    }

    public int getValidationCode() {
        return validationCode;
    }
}
