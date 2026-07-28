package com.clbs.portfolio.model;

/**
 * {@code 01 ERR-VSAM-STATUSES} and {@code 01 ERR-VSAM-MSGS} in
 * {@code src/copybook/common/ERRHAND.cpy}.
 *
 * <p>File status fields such as {@code WS-TRAN-STATUS} and {@code WS-PORT-STATUS} are two-byte
 * alphanumerics, so translated programs keep the raw status text; this enum names the four values
 * the copybook covers together with their standard message.
 */
public enum VsamStatus {

    /** {@code ERR-VSAM-SUCCESS VALUE '00'}. */
    SUCCESS("00", ""),
    /** {@code ERR-VSAM-EOF VALUE '10'}. */
    END_OF_FILE("10", ""),
    /** {@code ERR-VSAM-DUPKEY VALUE '22'} - {@code ERR-VSAM-22}. */
    DUPLICATE_KEY("22", "Duplicate record key"),
    /** {@code ERR-VSAM-NOTFND VALUE '23'} - {@code ERR-VSAM-23}. */
    NOT_FOUND("23", "Record not found");

    /** {@code ERR-OTHER} - the message for any status the copybook does not name. */
    public static final String OTHER_MESSAGE = "Unexpected VSAM error";

    public static final int LENGTH = 2;

    private final String code;
    private final String message;

    VsamStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    /** The copybook message for this status; empty for the two non-error statuses. */
    public String message() {
        return message;
    }

    /** The matching status, or {@code null} when the field holds an uncovered value. */
    public static VsamStatus fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (VsamStatus status : values()) {
            if (status.code.equals(stored)) {
                return status;
            }
        }
        return null;
    }

    /** The copybook message for a file status, falling back to {@link #OTHER_MESSAGE}. */
    public static String messageFor(String code) {
        VsamStatus status = fromCode(code);
        if (status == null || status.message.isEmpty()) {
            return OTHER_MESSAGE;
        }
        return status.message;
    }
}
