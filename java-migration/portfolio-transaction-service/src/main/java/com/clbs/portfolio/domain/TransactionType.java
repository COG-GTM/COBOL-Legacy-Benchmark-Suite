package com.clbs.portfolio.domain;

/**
 * Transaction type, mirroring the COBOL {@code TRN-TYPE PIC X(02)} field and its
 * 88-level condition names in copybook {@code TRNREC}:
 * <pre>
 *   88  TRN-TYPE-BUY     VALUE 'BU'.
 *   88  TRN-TYPE-SELL    VALUE 'SL'.
 *   88  TRN-TYPE-TRANS   VALUE 'TR'.
 *   88  TRN-TYPE-FEE     VALUE 'FE'.
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

    /** @return the two-character COBOL code (e.g. {@code "BU"}). */
    public String getCode() {
        return code;
    }

    /**
     * Resolve a raw two-character COBOL code to a type.
     *
     * @param code the raw {@code TRN-TYPE} value
     * @return the matching type, or {@code null} if it is not one of BU/SL/TR/FE
     *         (mirrors the {@code WHEN OTHER} branch of {@code 2120-CHECK-TRANSACTION-TYPE})
     */
    public static TransactionType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TransactionType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
