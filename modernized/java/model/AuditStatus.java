package com.clbs.portfolio.model;

/** Level-88 conditions on {@code AUD-STATUS PIC X(4)} in {@code src/copybook/common/AUDITLOG.cpy}. */
public enum AuditStatus {

    /** {@code 88 AUD-SUCCESS VALUE 'SUCC'}. */
    SUCCESS("SUCC"),
    /** {@code 88 AUD-FAILURE VALUE 'FAIL'}. */
    FAILURE("FAIL"),
    /** {@code 88 AUD-WARNING VALUE 'WARN'}. */
    WARNING("WARN");

    public static final int LENGTH = 4;

    private final String code;

    AuditStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** The matching status, or {@code null} when the buffer holds an uncovered value. */
    public static AuditStatus fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (AuditStatus status : values()) {
            if (status.code.equals(stored)) {
                return status;
            }
        }
        return null;
    }
}
