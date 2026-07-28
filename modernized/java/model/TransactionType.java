package com.clbs.portfolio.model;

/**
 * Level-88 conditions on {@code TRN-TYPE PIC X(02)} in {@code TRNREC.cpy}.
 *
 * <p>{@code TRN-TYPE} is a two-byte buffer that can legitimately hold an unrecognised value: the
 * validation in {@code PORTTRAN} paragraph {@code 2120-CHECK-TRANSACTION-TYPE} exists precisely to
 * detect one and echoes the raw bytes back in its error message. {@link TransactionRecord}
 * therefore keeps the raw code and exposes this enum as an interpretation of it, which may be
 * absent.
 */
public enum TransactionType {

    /** {@code 88 TRN-TYPE-BUY VALUE 'BU'}. */
    BUY("BU"),
    /** {@code 88 TRN-TYPE-SELL VALUE 'SL'}. */
    SELL("SL"),
    /** {@code 88 TRN-TYPE-TRANS VALUE 'TR'}. */
    TRANSFER("TR"),
    /** {@code 88 TRN-TYPE-FEE VALUE 'FE'}. */
    FEE("FE");

    public static final int LENGTH = 2;

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    /** The two-character value stored in {@code TRN-TYPE}. */
    public String code() {
        return code;
    }

    /** The matching type, or {@code null} when the buffer holds a value no level-88 covers. */
    public static TransactionType fromCode(String code) {
        String stored = CobolText.picX(code, LENGTH);
        for (TransactionType type : values()) {
            if (type.code.equals(stored)) {
                return type;
            }
        }
        return null;
    }

    /** Whether the buffer holds one of the four codes {@code 2120-CHECK-TRANSACTION-TYPE} accepts. */
    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }
}
