package com.cognition.portfolio.validation;

/**
 * The two fields PORTVALD writes back into its caller's parameter list:
 * {@code LS-RETURN-CODE PIC S9(4) COMP} and {@code LS-ERROR-MSG PIC X(50)}.
 *
 * @param returnCode   the numeric result, one of the VAL- codes in
 *                     {@link PortValConstants}
 * @param errorMessage the 50 byte message field, space filled on the right;
 *                     all spaces on success
 */
public record ValidationResult(int returnCode, String errorMessage) {

    public ValidationResult {
        errorMessage = CobolAlphanumeric.toFixedLength(errorMessage, PortValConstants.ERROR_MESSAGE_LENGTH);
    }

    public boolean isSuccess() {
        return returnCode == PortValConstants.VAL_SUCCESS;
    }

    /** The message without the COBOL padding, for logging and for APIs. */
    public String trimmedErrorMessage() {
        return errorMessage.stripTrailing();
    }
}
