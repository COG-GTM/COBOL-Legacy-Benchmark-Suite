package com.portfolio.domain;

import java.math.BigDecimal;

/**
 * Portfolio validation rules - migrated from COBOL PORTVAL.cpy.
 * Contains validation constants and result holder.
 */
public class PortfolioValidation {

    public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    public static final String ID_PREFIX = "PORT";

    public static final int VAL_SUCCESS = 0;
    public static final int VAL_INVALID_ID = 1;
    public static final int VAL_INVALID_ACCT = 2;
    public static final int VAL_INVALID_TYPE = 3;
    public static final int VAL_INVALID_AMT = 4;

    public static final String ERR_ID = "Invalid Portfolio ID format";
    public static final String ERR_ACCT = "Invalid Account Number format";
    public static final String ERR_TYPE = "Invalid Investment Type";
    public static final String ERR_AMT = "Amount outside valid range";

    private int errorCode;
    private String errorMessage;

    public PortfolioValidation() {
        this.errorCode = VAL_SUCCESS;
    }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isValid() { return errorCode == VAL_SUCCESS; }
}
