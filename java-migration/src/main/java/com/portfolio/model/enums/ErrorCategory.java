package com.portfolio.model.enums;

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
        for (ErrorCategory cat : values()) {
            if (cat.code.equals(code)) {
                return cat;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorCategory code: " + code);
    }
}
