package com.cog.portfolio.common;

/** ERRHAND.cpy ERR-CATEGORIES + RETHND.cpy ERROR-TYPE. */
public enum ErrorCategory implements CodedValue {
    VSAM("VS"),
    VALIDATION("VL"),
    PROCESSING("PR"),
    SYSTEM("SY"),
    DATABASE("DB"),
    SECURITY("SE");

    private final String code;

    ErrorCategory(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }
}
