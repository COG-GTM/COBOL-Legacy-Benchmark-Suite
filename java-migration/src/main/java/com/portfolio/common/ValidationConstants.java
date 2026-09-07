package com.portfolio.common;
import java.math.BigDecimal;
public final class ValidationConstants {
    private ValidationConstants() {}
    public static final String ID_PREFIX="PORT";
    public static final BigDecimal MIN_AMOUNT=new BigDecimal("-9999999999999.99");
    public static final BigDecimal MAX_AMOUNT=new BigDecimal("9999999999999.99");
    public static final int INVALID_ID=1, INVALID_ACCOUNT=2, INVALID_TYPE=3, INVALID_AMOUNT=4;
    public static final String INVALID_ID_MESSAGE="Invalid Portfolio ID format", INVALID_ACCOUNT_MESSAGE="Invalid Account Number format",
            INVALID_TYPE_MESSAGE="Invalid Investment Type", INVALID_AMOUNT_MESSAGE="Amount outside valid range";
}
