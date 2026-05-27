package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum PositionStatus {
    ACTIVE('A'),
    CLOSED('C'),
    PENDING('P');

    private final char code;

    PositionStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static PositionStatus fromCode(char code) {
        for (PositionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PositionStatus code: " + code);
    }

    @Converter(autoApply = true)
    public static class PositionStatusConverter implements AttributeConverter<PositionStatus, String> {

        @Override
        public String convertToDatabaseColumn(PositionStatus attribute) {
            return attribute == null ? null : String.valueOf(attribute.getCode());
        }

        @Override
        public PositionStatus convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : PositionStatus.fromCode(dbData.charAt(0));
        }
    }
}
