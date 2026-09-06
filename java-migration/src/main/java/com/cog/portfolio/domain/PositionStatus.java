package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** POSREC.cpy POS-STATUS level-88 values. */
public enum PositionStatus implements CodedValue {
    ACTIVE("A"),
    CLOSED("C"),
    PENDING("P");

    private final String code;

    PositionStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static PositionStatus fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<PositionStatus> {
        public Converter() {
            super(PositionStatus.values());
        }
    }
}
