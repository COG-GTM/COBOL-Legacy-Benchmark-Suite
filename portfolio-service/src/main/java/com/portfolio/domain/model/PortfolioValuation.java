package com.portfolio.domain.model;

import java.math.BigDecimal;

/**
 * Value object containing validation constants from PORTVAL.cpy.
 * Maps the VAL-CONSTANTS section: min/max amounts and ID prefix.
 */
public final class PortfolioValuation {

    public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    public static final String ID_PREFIX = "PORT";

    public static final int VAL_SUCCESS = 0;
    public static final int VAL_INVALID_ID = 1;
    public static final int VAL_INVALID_ACCT = 2;
    public static final int VAL_INVALID_TYPE = 3;
    public static final int VAL_INVALID_AMT = 4;

    private PortfolioValuation() {}

    public static boolean isValidPortfolioId(String id) {
        return id != null && id.length() <= 8 && id.startsWith(ID_PREFIX);
    }

    public static boolean isValidAccountNumber(String accountNo) {
        return accountNo != null && !accountNo.isBlank() && accountNo.length() <= 10;
    }

    public static boolean isAmountInRange(BigDecimal amount) {
        if (amount == null) return false;
        return amount.compareTo(MIN_AMOUNT) >= 0 && amount.compareTo(MAX_AMOUNT) <= 0;
    }
}
