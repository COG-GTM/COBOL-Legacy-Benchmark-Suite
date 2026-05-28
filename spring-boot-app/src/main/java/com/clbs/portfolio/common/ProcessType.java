package com.clbs.portfolio.common;

import lombok.Getter;

/**
 * Process types from COBOL BCHCON.cpy (BCT-PROC-TYPES).
 */
@Getter
public enum ProcessType {

    INITIAL("INI"),
    UPDATE("UPD"),
    REPORT("RPT"),
    CLEANUP("CLN");

    private final String code;

    ProcessType(String code) {
        this.code = code;
    }

    public static ProcessType fromCode(String code) {
        for (ProcessType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown process type code: " + code);
    }
}
