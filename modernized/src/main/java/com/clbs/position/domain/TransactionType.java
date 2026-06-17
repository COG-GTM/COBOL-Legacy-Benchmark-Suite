package com.clbs.position.domain;

/**
 * Transaction type, ported from the COBOL 88-level condition names on
 * {@code TRN-TYPE} in copybook {@code src/copybook/common/TRNREC.cpy}:
 *
 * <pre>
 *   10  TRN-TYPE           PIC X(02).
 *       88  TRN-TYPE-BUY     VALUE 'BU'.
 *       88  TRN-TYPE-SELL    VALUE 'SL'.
 *       88  TRN-TYPE-TRANS   VALUE 'TR'.
 *       88  TRN-TYPE-FEE     VALUE 'FE'.
 * </pre>
 */
public enum TransactionType {
    BUY("BU"),
    SELL("SL"),
    TRANSFER("TR"),
    FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    /** The 2-character COBOL {@code TRN-TYPE} code. */
    public String code() {
        return code;
    }

    /** Resolves a {@link TransactionType} from its COBOL 2-character code. */
    public static TransactionType fromCode(String code) {
        if (code != null) {
            String trimmed = code.trim();
            for (TransactionType type : values()) {
                if (type.code.equals(trimmed)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Invalid transaction type code: '" + code + "'");
    }
}
