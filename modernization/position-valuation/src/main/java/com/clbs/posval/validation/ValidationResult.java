package com.clbs.posval.validation;

import com.clbs.posval.cobol.CobolString;

/**
 * The {@code LS-RETURN-CODE} / {@code LS-ERROR-MSG} pair that {@code PORTVALD} passes back through
 * its linkage section.
 *
 * @param returnCode one of the {@code VAL-RETURN-CODES} of the PORTVAL copybook
 * @param message {@code LS-ERROR-MSG PIC X(50)}, space filled on success
 */
public record ValidationResult(int returnCode, String message) {

    /** {@code VAL-SUCCESS PIC S9(4) VALUE +0}. */
    public static final int VAL_SUCCESS = 0;
    /** {@code VAL-INVALID-ID PIC S9(4) VALUE +1}. */
    public static final int VAL_INVALID_ID = 1;
    /** {@code VAL-INVALID-ACCT PIC S9(4) VALUE +2}. */
    public static final int VAL_INVALID_ACCT = 2;
    /** {@code VAL-INVALID-TYPE PIC S9(4) VALUE +3}. */
    public static final int VAL_INVALID_TYPE = 3;
    /** {@code VAL-INVALID-AMT PIC S9(4) VALUE +4}. */
    public static final int VAL_INVALID_AMT = 4;

    public static final String ERR_ID = "Invalid Portfolio ID format";
    public static final String ERR_ACCT = "Invalid Account Number format";
    public static final String ERR_TYPE = "Invalid Investment Type";
    public static final String ERR_AMT = "Amount outside valid range";
    public static final String ERR_VALIDATE_TYPE = "Invalid validation type";

    public static final int MESSAGE_WIDTH = 50;

    public ValidationResult {
        message = CobolString.move(message, MESSAGE_WIDTH);
    }

    public static ValidationResult success() {
        return new ValidationResult(VAL_SUCCESS, "");
    }

    public boolean isSuccess() {
        return returnCode == VAL_SUCCESS;
    }

    /** The message with the {@code PIC X(50)} space padding removed. */
    public String trimmedMessage() {
        return message.stripTrailing();
    }
}
