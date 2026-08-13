package com.ipms.domain;

/** ERR-CATEGORIES from ERRHAND.cpy (VS=VSAM, VL=Validation, PR=Processing, SY=System). */
public enum ErrorCategory {
    VSAM("VS"),
    VALIDATION("VL"),
    PROCESSING("PR"),
    SYSTEM("SY");

    private final String code;

    ErrorCategory(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ErrorCategory fromCode(String code) {
        for (ErrorCategory c : values()) {
            if (c.code.equals(code)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown error category: " + code);
    }
}
