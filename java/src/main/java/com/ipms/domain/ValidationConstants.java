package com.ipms.domain;

import java.math.BigDecimal;

/**
 * Portfolio validation return codes, messages and constants from
 * {@code src/copybook/common/PORTVAL.cpy}.
 */
public final class ValidationConstants {

    private ValidationConstants() {
    }

    // VAL-RETURN-CODES
    public static final int VAL_SUCCESS = 0;
    public static final int VAL_INVALID_ID = 1;
    public static final int VAL_INVALID_ACCT = 2;
    public static final int VAL_INVALID_TYPE = 3;
    public static final int VAL_INVALID_AMT = 4;

    // VAL-ERROR-MESSAGES
    public static final String VAL_ERR_ID = "Invalid Portfolio ID format";
    public static final String VAL_ERR_ACCT = "Invalid Account Number format";
    public static final String VAL_ERR_TYPE = "Invalid Investment Type";
    public static final String VAL_ERR_AMT = "Amount outside valid range";

    // VAL-CONSTANTS
    public static final BigDecimal VAL_MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    public static final BigDecimal VAL_MAX_AMOUNT = new BigDecimal("9999999999999.99");
    public static final String VAL_ID_PREFIX = "PORT";
}
