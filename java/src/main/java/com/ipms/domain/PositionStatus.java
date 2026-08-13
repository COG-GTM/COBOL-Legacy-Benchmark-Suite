package com.ipms.domain;

/** POS-STATUS level-88 values from POSREC.cpy (A=ACTIVE, C=CLOSED, P=PENDING). */
public enum PositionStatus {
    ACTIVE("A"),
    CLOSED("C"),
    PENDING("P");

    private final String code;

    PositionStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static PositionStatus fromCode(String code) {
        for (PositionStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown position status: " + code);
    }
}
