package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** PORTFLIO.cpy PORT-STATUS level-88 values. */
public enum PortfolioStatus implements CodedValue {
    ACTIVE("A"),
    CLOSED("C"),
    SUSPENDED("S");

    private final String code;

    PortfolioStatus(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static PortfolioStatus fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<PortfolioStatus> {
        public Converter() {
            super(PortfolioStatus.values());
        }
    }
}
