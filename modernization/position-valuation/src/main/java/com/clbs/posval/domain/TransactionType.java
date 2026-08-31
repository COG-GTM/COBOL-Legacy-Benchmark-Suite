package com.clbs.posval.domain;

import java.util.Optional;

/**
 * {@code TRN-TYPE PIC X(02)} of the TRNREC copybook and its 88-levels.
 *
 * <p>The COMMON copybook declares the same four codes as literals
 * ({@code TRN-TYPE-BUY 'BU'} …), duplicating the TRNREC condition names.
 */
public enum TransactionType {
    /** {@code TRN-TYPE-BUY VALUE 'BU'}. */
    BUY("BU"),
    /** {@code TRN-TYPE-SELL VALUE 'SL'}. */
    SELL("SL"),
    /** {@code TRN-TYPE-TRANS VALUE 'TR'} — transfer; not implemented by PORTTRAN. */
    TRANSFER("TR"),
    /** {@code TRN-TYPE-FEE VALUE 'FE'}. */
    FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** Empty for any code outside the four 88-levels, which PORTTRAN 2120 rejects. */
    public static Optional<TransactionType> fromCode(String code) {
        for (TransactionType type : values()) {
            if (type.code.equals(code)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
