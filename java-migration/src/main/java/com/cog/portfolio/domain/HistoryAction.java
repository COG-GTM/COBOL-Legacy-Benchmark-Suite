package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** HISTREC.cpy HIST-ACTION-CODE level-88 values. */
public enum HistoryAction implements CodedValue {
    ADD("A"),
    CHANGE("C"),
    DELETE("D");

    private final String code;

    HistoryAction(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static HistoryAction fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<HistoryAction> {
        public Converter() {
            super(HistoryAction.values());
        }
    }
}
