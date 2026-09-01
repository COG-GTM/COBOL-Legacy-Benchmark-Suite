package com.clbs.portfolio.domain.enums;

public enum PositionStatus {
    ACTIVE("A"),
    CLOSED("C"),
    PENDING("P");

    private final String code;

    PositionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PositionStatus fromCode(String code) {
        for (PositionStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown position status code: " + code);
    }
}
