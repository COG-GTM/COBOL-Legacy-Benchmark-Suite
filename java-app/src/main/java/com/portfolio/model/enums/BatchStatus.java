package com.portfolio.model.enums;

/**
 * Batch control status codes.
 * Migrated from: COBOL level-88 conditions in BCHCTL.cpy (lines 16-20).
 * R=Ready, A=Active, W=Waiting, D=Done, E=Error
 */
public enum BatchStatus {
    READY("R", "Ready"),
    ACTIVE("A", "Active"),
    WAITING("W", "Waiting"),
    DONE("D", "Done"),
    ERROR("E", "Error");

    private final String code;
    private final String description;

    BatchStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
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
