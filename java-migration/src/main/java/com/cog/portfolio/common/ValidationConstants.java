package com.cog.portfolio.common;

import java.math.BigDecimal;
import java.util.Set;

/** PORTVAL.cpy VALIDATION-CONSTANTS. */
public final class ValidationConstants {

    /** VAL-MIN-AMOUNT PIC S9(13)V99 VALUE -9999999999999.99 */
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    /** VAL-MAX-AMOUNT PIC S9(13)V99 VALUE +9999999999999.99 */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    /** VAL-ID-PREFIX PIC X(4) VALUE 'PORT' */
    public static final String ID_PREFIX = "PORT";
    public static final int ID_LENGTH = 8;
    public static final int ACCOUNT_LENGTH = 10;
    /** VAL-TYPES: STK, BND, MMF, ETF */
    public static final Set<String> INVESTMENT_TYPES = Set.of("STK", "BND", "MMF", "ETF");

    private ValidationConstants() {
    }
}
