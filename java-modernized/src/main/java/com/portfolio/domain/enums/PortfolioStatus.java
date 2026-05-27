package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum PortfolioStatus {
    ACTIVE('A'),
    CLOSED('C'),
    SUSPENDED('S');

    private final char code;

    PortfolioStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static PortfolioStatus fromCode(char code) {
        for (PortfolioStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PortfolioStatus code: " + code);
    }

    @Converter(autoApply = true)
    public static class PortfolioStatusConverter implements AttributeConverter<PortfolioStatus, String> {

        @Override
        public String convertToDatabaseColumn(PortfolioStatus attribute) {
            return attribute == null ? null : String.valueOf(attribute.getCode());
        }

        @Override
        public PortfolioStatus convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : PortfolioStatus.fromCode(dbData.charAt(0));
        }
    }
}
