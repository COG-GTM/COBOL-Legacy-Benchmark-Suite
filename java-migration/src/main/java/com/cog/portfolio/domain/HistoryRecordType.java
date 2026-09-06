package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** HISTREC.cpy HIST-RECORD-TYPE level-88 values. */
public enum HistoryRecordType implements CodedValue {
    PORTFOLIO("PT"),
    POSITION("PS"),
    TRANSACTION("TR");

    private final String code;

    HistoryRecordType(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static HistoryRecordType fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<HistoryRecordType> {
        public Converter() {
            super(HistoryRecordType.values());
        }
    }
}
