package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum HistoryActionCode {
    ADD('A'),
    CHANGE('C'),
    DELETE('D');

    private final char code;

    HistoryActionCode(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static HistoryActionCode fromCode(char code) {
        for (HistoryActionCode action : values()) {
            if (action.code == code) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown HistoryActionCode code: " + code);
    }

    @Converter(autoApply = true)
    public static class HistoryActionCodeConverter implements AttributeConverter<HistoryActionCode, String> {

        @Override
        public String convertToDatabaseColumn(HistoryActionCode attribute) {
            return attribute == null ? null : String.valueOf(attribute.getCode());
        }

        @Override
        public HistoryActionCode convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : HistoryActionCode.fromCode(dbData.charAt(0));
        }
    }
}
