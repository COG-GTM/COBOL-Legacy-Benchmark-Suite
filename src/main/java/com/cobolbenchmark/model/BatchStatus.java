package com.cobolbenchmark.model;

/**
 * Batch Status enum - from BCHCTL.cpy level-88 conditions.
 * 88 BCT-STAT-READY   VALUE 'R'.
 * 88 BCT-STAT-ACTIVE  VALUE 'A'.
 * 88 BCT-STAT-WAITING VALUE 'W'.
 * 88 BCT-STAT-DONE    VALUE 'D'.
 * 88 BCT-STAT-ERROR   VALUE 'E'.
 */
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

    public String getCode() {
        return code;
    }

    public static BatchStatus fromCode(String code) {
        for (BatchStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown batch status code: " + code);
    }
}
