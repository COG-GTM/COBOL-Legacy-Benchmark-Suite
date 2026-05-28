package com.clbs.portfolio.common;

import lombok.Getter;

/**
 * Batch control status values from COBOL BCHCTL.cpy (BCT-STATUS level 88s)
 * and BCHCON.cpy (BCT-STAT-VALUES).
 */
@Getter
public enum BatchStatus {

    READY("R"),
    ACTIVE("A"),
    WAITING("W"),
    DONE("D"),
    ERROR("E");

    private final String code;

    BatchStatus(String code) {
        this.code = code;
    }

    public static BatchStatus fromCode(String code) {
        for (BatchStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown batch status code: " + code);
    }
}
