package com.cognition.portfolio.validation;

import java.util.Objects;

/**
 * Outputs PORTVALD writes back into the caller's {@code LS-VALIDATION-REQUEST}.
 *
 * @param returnCode   value of {@code LS-RETURN-CODE PIC S9(4) COMP}
 * @param errorMessage value of {@code LS-ERROR-MSG PIC X(50)}: always exactly 50 characters,
 *                     space-padded; all spaces on success
 */
public record ValidationResult(int returnCode, String errorMessage) {

    public ValidationResult {
        Objects.requireNonNull(errorMessage, "errorMessage");
        if (errorMessage.length() != PortvalConstants.FIELD_LENGTH) {
            throw new IllegalArgumentException("errorMessage must be exactly "
                    + PortvalConstants.FIELD_LENGTH + " characters");
        }
    }

    static ValidationResult of(int returnCode, String errorMessage) {
        return new ValidationResult(returnCode, errorMessage);
    }

    public boolean isSuccess() {
        return returnCode == PortvalConstants.VAL_SUCCESS;
    }
}
