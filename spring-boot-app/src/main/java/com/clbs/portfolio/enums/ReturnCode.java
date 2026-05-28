package com.clbs.portfolio.enums;

public enum ReturnCode {
    VAL_SUCCESS(0, "Validation successful"),
    VAL_INVALID_ID(1, "Invalid Portfolio ID format"),
    VAL_INVALID_ACCT(2, "Invalid Account Number format"),
    VAL_INVALID_TYPE(3, "Invalid Investment Type"),
    VAL_INVALID_AMT(4, "Amount outside valid range");

    private final int code;
    private final String message;

    ReturnCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
