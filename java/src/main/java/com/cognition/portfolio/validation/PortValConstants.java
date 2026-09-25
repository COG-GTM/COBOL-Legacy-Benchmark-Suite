package com.cognition.portfolio.validation;

import java.math.BigDecimal;

/**
 * Java transcription of the copybook {@code src/copybook/common/PORTVAL.cpy}.
 * Names, values and field lengths are carried over unchanged so that a
 * reviewer can hold the two files side by side.
 */
public final class PortValConstants {

    private PortValConstants() {
    }

    /** {@code LS-INPUT-VALUE PIC X(50)}. */
    public static final int INPUT_VALUE_LENGTH = 50;

    /** {@code LS-ERROR-MSG PIC X(50)}. */
    public static final int ERROR_MESSAGE_LENGTH = 50;

    /** {@code VAL-NUMERIC-CHECK PIC X(10)}. */
    public static final int NUMERIC_CHECK_LENGTH = 10;

    /** Integer digit positions of {@code VAL-TEMP-NUM PIC S9(13)V99}. */
    public static final int TEMP_NUM_INTEGER_DIGITS = 13;

    /** Fraction digit positions of {@code VAL-TEMP-NUM PIC S9(13)V99}. */
    public static final int TEMP_NUM_FRACTION_DIGITS = 2;

    // Validation return codes: VAL-RETURN-CODES.
    public static final int VAL_SUCCESS = 0;
    public static final int VAL_INVALID_ID = 1;
    public static final int VAL_INVALID_ACCT = 2;
    public static final int VAL_INVALID_TYPE = 3;
    public static final int VAL_INVALID_AMT = 4;

    // Validation error messages: VAL-ERROR-MESSAGES, each PIC X(50).
    public static final String VAL_ERR_ID = pad("Invalid Portfolio ID format");
    public static final String VAL_ERR_ACCT = pad("Invalid Account Number format");
    public static final String VAL_ERR_TYPE = pad("Invalid Investment Type");
    public static final String VAL_ERR_AMT = pad("Amount outside valid range");

    /** Literal moved by 0000-MAIN when LS-VALIDATE-TYPE matches no condition name. */
    public static final String ERR_UNKNOWN_VALIDATE_TYPE = pad("Invalid validation type");

    /** {@code SPACES} moved to LS-ERROR-MSG on success. */
    public static final String SPACES = pad("");

    // Validation constants: VAL-CONSTANTS.
    public static final BigDecimal VAL_MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    public static final BigDecimal VAL_MAX_AMOUNT = new BigDecimal("9999999999999.99");
    public static final String VAL_ID_PREFIX = "PORT";

    private static String pad(String literal) {
        return CobolAlphanumeric.toFixedLength(literal, ERROR_MESSAGE_LENGTH);
    }
}
