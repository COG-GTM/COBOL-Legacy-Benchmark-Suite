package com.portfolio.model.copybook;

import java.math.BigDecimal;

/**
 * Migrated from copybook {@code src/copybook/common/PORTVAL.cpy}
 * (validation return codes, error messages, and constants).
 */
public final class PortfolioValidation {

    private PortfolioValidation() {}

    // Validation return codes (VAL-RETURN-CODES, PIC S9(4))
    public static final int VAL_SUCCESS = 0;
    public static final int VAL_INVALID_ID = 1;
    public static final int VAL_INVALID_ACCT = 2;
    public static final int VAL_INVALID_TYPE = 3;
    public static final int VAL_INVALID_AMT = 4;

    // Validation error messages (VAL-ERROR-MESSAGES, PIC X(50))
    public static final String ERR_ID = "Invalid Portfolio ID format";
    public static final String ERR_ACCT = "Invalid Account Number format";
    public static final String ERR_TYPE = "Invalid Investment Type";
    public static final String ERR_AMT = "Amount outside valid range";

    // Validation constants (VAL-CONSTANTS)
    /** VAL-MIN-AMOUNT PIC S9(13)V99. */
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    /** VAL-MAX-AMOUNT PIC S9(13)V99. */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    /** VAL-ID-PREFIX PIC X(4). */
    public static final String ID_PREFIX = "PORT";
}
