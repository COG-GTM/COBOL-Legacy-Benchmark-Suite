package com.clbs.portfolio.model;

/** Level-88 conditions on {@code AUD-TYPE PIC X(4)} in {@code src/copybook/common/AUDITLOG.cpy}. */
public enum AuditType {

    /** {@code 88 AUD-TRANSACTION VALUE 'TRAN'} - the type {@code PORTTRAN} always writes. */
    TRANSACTION("TRAN"),
    /** {@code 88 AUD-USER-ACTION VALUE 'USER'}. */
    USER_ACTION("USER"),
    /** {@code 88 AUD-SYSTEM-EVENT VALUE 'SYST'}. */
    SYSTEM_EVENT("SYST");

    public static final int LENGTH = 4;

    private final String code;

    AuditType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** The matching type, or {@code null} when the buffer holds an uncovered value. */
    public static AuditType fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (AuditType type : values()) {
            if (type.code.equals(stored)) {
                return type;
            }
        }
        return null;
    }
}
