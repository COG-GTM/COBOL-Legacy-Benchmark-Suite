package com.portfolio.model.enums;

/**
 * Process mode codes.
 * Migrated from: BCHCTL00.cbl working storage (lines 41-45).
 * I=Initialize, C=CheckPrereq, U=UpdateStatus, F=Finalize
 */
public enum ProcessMode {
    INITIALIZE("I", "Initialize"),
    CHECK_PREREQ("C", "Check Prerequisites"),
    UPDATE_STATUS("U", "Update Status"),
    FINALIZE("F", "Finalize");

    private final String code;
    private final String description;

    ProcessMode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ProcessMode fromCode(String code) {
        for (ProcessMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown process mode code: " + code);
    }
}
