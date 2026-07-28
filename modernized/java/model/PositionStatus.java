package com.clbs.portfolio.model;

/** Level-88 conditions on {@code POS-STATUS PIC X(01)} in {@code POSREC.cpy}. */
public enum PositionStatus {

    /** {@code 88 POS-STATUS-ACTIVE VALUE 'A'}. */
    ACTIVE("A"),
    /** {@code 88 POS-STATUS-CLOSED VALUE 'C'}. */
    CLOSED("C"),
    /** {@code 88 POS-STATUS-PEND VALUE 'P'}. */
    PENDING("P");

    public static final int LENGTH = 1;

    private final String code;

    PositionStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** The matching status, or {@code null} when the buffer holds an uncovered value. */
    public static PositionStatus fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (PositionStatus status : values()) {
            if (status.code.equals(stored)) {
                return status;
            }
        }
        return null;
    }
}
