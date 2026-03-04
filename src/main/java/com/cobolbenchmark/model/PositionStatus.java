package com.cobolbenchmark.model;

/**
 * Position Status enum - from POSREC.cpy level-88 conditions.
 * 88 POS-STATUS-ACTIVE VALUE 'A'.
 * 88 POS-STATUS-CLOSED VALUE 'C'.
 * 88 POS-STATUS-PEND   VALUE 'P'.
 */
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
        for (PositionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown position status code: " + code);
    }
}
