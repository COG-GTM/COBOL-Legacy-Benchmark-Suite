package com.cog.portfolio.common;

/** COMMON.cpy STATUS-CODES. */
public enum StatusCode implements CodedValue {
    ACTIVE("A"),
    CLOSED("C"),
    PENDING("P"),
    SUSPENDED("S"),
    FAILED("F"),
    REVERSED("R");

    private final String code;

    StatusCode(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static StatusCode fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }
}
