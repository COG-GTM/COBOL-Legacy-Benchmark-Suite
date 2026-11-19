package com.clbs.portfolio.domain;

import java.util.Arrays;
import java.util.Optional;

/** TRN-TYPE level-88 values from TRNREC.cpy. */
public enum TransactionType {
    BUY("BU"), SELL("SL"), TRANSFER("TR"), FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Optional<TransactionType> fromCode(String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst();
    }
}
