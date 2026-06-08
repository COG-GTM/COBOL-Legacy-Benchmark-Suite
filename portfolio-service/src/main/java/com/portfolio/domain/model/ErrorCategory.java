package com.portfolio.domain.model;

/**
 * Maps COBOL error categories from ERRHAND.cpy ERR-CATEGORIES.
 */
public enum ErrorCategory {
    VSAM("VS"),
    VALIDATION("VL"),
    PROCESSING("PR"),
    SYSTEM("SY");

    private final String code;

    ErrorCategory(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ErrorCategory fromCode(String code) {
        for (ErrorCategory c : values()) {
            if (c.code.equals(code)) return c;
        }
        throw new IllegalArgumentException("Unknown error category code: " + code);
    }
}
