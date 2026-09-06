package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** TRNREC.cpy / COMMON.cpy TRN-TYPE level-88 values. */
public enum TransactionType implements CodedValue {
    BUY("BU"),
    SELL("SL"),
    TRANSFER("TR"),
    FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static TransactionType fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<TransactionType> {
        public Converter() {
            super(TransactionType.values());
        }
    }
}
