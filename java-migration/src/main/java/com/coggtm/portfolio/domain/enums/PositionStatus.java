package com.coggtm.portfolio.domain.enums;

/**
 * Maps to COBOL POS-STATUS 88-level conditions in POSREC.cpy.
 */
public enum PositionStatus {
    A("Active"),
    C("Closed"),
    P("Pending");

    private final String description;

    PositionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
