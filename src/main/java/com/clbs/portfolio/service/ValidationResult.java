package com.clbs.portfolio.service;

/** LS-RETURN-CODE / LS-ERROR-MSG returned by PORTVALD. */
public record ValidationResult(int returnCode, String message) {

    public static final int VAL_SUCCESS = 0;
    public static final int VAL_INVALID_ID = 1;
    public static final int VAL_INVALID_ACCT = 2;
    public static final int VAL_INVALID_TYPE = 3;
    public static final int VAL_INVALID_AMT = 4;

    public boolean valid() {
        return returnCode == VAL_SUCCESS;
    }
}
