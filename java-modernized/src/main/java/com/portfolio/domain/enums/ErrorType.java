package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ErrorType {
    SYSTEM('S'),
    APPLICATION('A'),
    DATA('D');

    private final char code;

    ErrorType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ErrorType fromCode(char code) {
        for (ErrorType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorType code: " + code);
    }

    @Converter(autoApply = true)
    public static class ErrorTypeConverter implements AttributeConverter<ErrorType, String> {

        @Override
        public String convertToDatabaseColumn(ErrorType attribute) {
            return attribute == null ? null : String.valueOf(attribute.getCode());
        }

        @Override
        public ErrorType convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : ErrorType.fromCode(dbData.charAt(0));
        }
    }
}
