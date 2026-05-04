package com.portfolio.entity;

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
        for (ErrorCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorCategory code: " + code);
    }
}
