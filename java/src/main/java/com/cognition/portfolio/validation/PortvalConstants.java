package com.cognition.portfolio.validation;

import java.math.BigDecimal;

/**
 * Constants from copybook {@code PORTVAL} (src/copybook/common/PORTVAL.cpy).
 *
 * <p>Error messages are held as the full {@code PIC X(50)} field value, i.e. right-padded
 * with spaces to 50 characters, exactly as PORTVALD moves them into {@code LS-ERROR-MSG}.
 */
public final class PortvalConstants {

    /** {@code VAL-SUCCESS PIC S9(4) VALUE +0}. */
    public static final int VAL_SUCCESS = 0;
    /** {@code VAL-INVALID-ID PIC S9(4) VALUE +1}; also returned for an unknown validation type. */
    public static final int VAL_INVALID_ID = 1;
    /** {@code VAL-INVALID-ACCT PIC S9(4) VALUE +2}. */
    public static final int VAL_INVALID_ACCT = 2;
    /** {@code VAL-INVALID-TYPE PIC S9(4) VALUE +3}. */
    public static final int VAL_INVALID_TYPE = 3;
    /** {@code VAL-INVALID-AMT PIC S9(4) VALUE +4}. */
    public static final int VAL_INVALID_AMT = 4;

    /** Length of {@code LS-INPUT-VALUE} and {@code LS-ERROR-MSG}. */
    public static final int FIELD_LENGTH = 50;

    /** {@code VAL-ERR-ID PIC X(50)}. */
    public static final String VAL_ERR_ID = AlphanumericField.pad("Invalid Portfolio ID format", FIELD_LENGTH);
    /** {@code VAL-ERR-ACCT PIC X(50)}. */
    public static final String VAL_ERR_ACCT = AlphanumericField.pad("Invalid Account Number format", FIELD_LENGTH);
    /** {@code VAL-ERR-TYPE PIC X(50)}. */
    public static final String VAL_ERR_TYPE = AlphanumericField.pad("Invalid Investment Type", FIELD_LENGTH);
    /** {@code VAL-ERR-AMT PIC X(50)}. */
    public static final String VAL_ERR_AMT = AlphanumericField.pad("Amount outside valid range", FIELD_LENGTH);
    /** Literal moved to {@code LS-ERROR-MSG} by {@code WHEN OTHER} in PORTVALD 0000-MAIN. */
    public static final String ERR_INVALID_VALIDATION_TYPE =
            AlphanumericField.pad("Invalid validation type", FIELD_LENGTH);
    /** {@code MOVE SPACES TO LS-ERROR-MSG} on success. */
    public static final String NO_ERROR = AlphanumericField.pad("", FIELD_LENGTH);

    /** {@code VAL-MIN-AMOUNT PIC S9(13)V99 VALUE -9999999999999.99}. */
    public static final BigDecimal VAL_MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    /** {@code VAL-MAX-AMOUNT PIC S9(13)V99 VALUE +9999999999999.99}. */
    public static final BigDecimal VAL_MAX_AMOUNT = new BigDecimal("9999999999999.99");
    /** {@code VAL-ID-PREFIX PIC X(4) VALUE 'PORT'}. */
    public static final String VAL_ID_PREFIX = "PORT";

    /** Integer digits of {@code VAL-TEMP-NUM PIC S9(13)V99}. */
    public static final int TEMP_NUM_INTEGER_DIGITS = 13;
    /** Decimal digits of {@code VAL-TEMP-NUM PIC S9(13)V99}. */
    public static final int TEMP_NUM_SCALE = 2;
    /** Length of {@code VAL-NUMERIC-CHECK PIC X(10)}. */
    public static final int NUMERIC_CHECK_LENGTH = 10;

    private PortvalConstants() {
    }
}
