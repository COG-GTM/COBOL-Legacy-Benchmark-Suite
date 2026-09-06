package com.cog.portfolio.domain;

import com.cog.portfolio.common.CodedValue;
import com.cog.portfolio.common.CodedValueConverter;

/** PORTFLIO.cpy PORT-CLIENT-TYPE level-88 values. */
public enum ClientType implements CodedValue {
    INDIVIDUAL("I"),
    CORPORATE("C"),
    TRUST("T");

    private final String code;

    ClientType(String code) {
        this.code = code;
    }

    @Override
    public String code() {
        return code;
    }

    public static ClientType fromCode(String code) {
        return CodedValue.fromCode(values(), code);
    }

    @jakarta.persistence.Converter
    public static class Converter extends CodedValueConverter<ClientType> {
        public Converter() {
            super(ClientType.values());
        }
    }
}
