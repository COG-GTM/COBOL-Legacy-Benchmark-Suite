package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** TRNREC.cpy TRN-STATUS level-88 values. */
public enum TransactionStatus implements CodedValue {
    PENDING("P"),
    DONE("D"),
    FAILED("F"),
    REVERSED("R");

    private final String code;

    TransactionStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static TransactionStatus fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<TransactionStatus> {
        public Converter() {
            super(TransactionStatus.values());
        }
    }
}
