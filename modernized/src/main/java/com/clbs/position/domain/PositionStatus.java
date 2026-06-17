package com.clbs.position.domain;

/**
 * Position status, ported from the COBOL 88-level condition names on
 * {@code POS-STATUS} in copybook {@code src/copybook/common/POSREC.cpy}:
 *
 * <pre>
 *   10  POS-STATUS         PIC X(01).
 *       88  POS-STATUS-ACTIVE  VALUE 'A'.
 *       88  POS-STATUS-CLOSED  VALUE 'C'.
 *       88  POS-STATUS-PEND    VALUE 'P'.
 * </pre>
 */
public enum PositionStatus {
    ACTIVE("A"),
    CLOSED("C"),
    PENDING("P");

    private final String code;

    PositionStatus(String code) {
        this.code = code;
    }

    /** The 1-character COBOL {@code POS-STATUS} code. */
    public String code() {
        return code;
    }

    public static PositionStatus fromCode(String code) {
        if (code != null) {
            String trimmed = code.trim();
            for (PositionStatus status : values()) {
                if (status.code.equals(trimmed)) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Invalid position status code: '" + code + "'");
    }
}
