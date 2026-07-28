package com.clbs.portfolio.model;

/**
 * Level-88 conditions on {@code AUD-ACTION PIC X(8)} in {@code src/copybook/common/AUDITLOG.cpy}.
 *
 * <p>The copybook spells the values out to the full eight bytes ({@code VALUE 'CREATE  '}), which is
 * the same buffer content as the padded code held here.
 */
public enum AuditAction {

    /** {@code 88 AUD-CREATE VALUE 'CREATE  '} - written for a buy. */
    CREATE("CREATE"),
    /** {@code 88 AUD-UPDATE VALUE 'UPDATE  '} - written for a transfer or a fee. */
    UPDATE("UPDATE"),
    /** {@code 88 AUD-DELETE VALUE 'DELETE  '} - written for a sell. */
    DELETE("DELETE"),
    /** {@code 88 AUD-INQUIRE VALUE 'INQUIRE '}. */
    INQUIRE("INQUIRE"),
    /** {@code 88 AUD-LOGIN VALUE 'LOGIN   '}. */
    LOGIN("LOGIN"),
    /** {@code 88 AUD-LOGOUT VALUE 'LOGOUT  '}. */
    LOGOUT("LOGOUT"),
    /** {@code 88 AUD-STARTUP VALUE 'STARTUP '}. */
    STARTUP("STARTUP"),
    /** {@code 88 AUD-SHUTDOWN VALUE 'SHUTDOWN'}. */
    SHUTDOWN("SHUTDOWN");

    public static final int LENGTH = 8;

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    /** The action as stored in the eight-byte field, space-padded. */
    public String code() {
        return CobolText.picX(code, LENGTH);
    }

    /** The matching action, or {@code null} when the buffer holds an uncovered value. */
    public static AuditAction fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (AuditAction action : values()) {
            if (action.code().equals(stored)) {
                return action;
            }
        }
        return null;
    }
}
