package com.clbs.portfolio.domain.enums;

public enum HistoryActionCode {
    ADD("A"),
    CHANGE("C"),
    DELETE("D");

    private final String code;

    HistoryActionCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static HistoryActionCode fromCode(String code) {
        for (HistoryActionCode value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown history action code: " + code);
    }
}
