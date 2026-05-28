package com.clbs.portfolio.common;

import lombok.Getter;

/**
 * Entity status codes from COBOL COMMON.cpy (STATUS-CODES).
 */
@Getter
public enum EntityStatus {

    ACTIVE("A"),
    CLOSED("C"),
    PENDING("P"),
    SUSPENDED("S"),
    FAILED("F"),
    REVERSED("R");

    private final String code;

    EntityStatus(String code) {
        this.code = code;
    }

    public static EntityStatus fromCode(String code) {
        for (EntityStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown entity status code: " + code);
    }
}
