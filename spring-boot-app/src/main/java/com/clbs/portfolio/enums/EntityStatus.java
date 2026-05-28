package com.clbs.portfolio.enums;

public enum EntityStatus {
    ACTIVE("A"),
    CLOSED("C"),
    SUSPENDED("S"),
    PENDING("P");

    private final String code;

    EntityStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static EntityStatus fromCode(String code) {
        for (EntityStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
