package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ClientType {
    INDIVIDUAL('I'),
    CORPORATE('C'),
    TRUST('T');

    private final char code;

    ClientType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static ClientType fromCode(char code) {
        for (ClientType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ClientType code: " + code);
    }

    @Converter(autoApply = true)
    public static class ClientTypeConverter implements AttributeConverter<ClientType, String> {

        @Override
        public String convertToDatabaseColumn(ClientType attribute) {
            return attribute == null ? null : String.valueOf(attribute.getCode());
        }

        @Override
        public ClientType convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : ClientType.fromCode(dbData.charAt(0));
        }
    }
}
