package com.portfolio.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the 88-level conditions from PORTFLIO.cpy:
 * <pre>
 *     10  PORT-STATUS         PIC X(1).
 *         88  PORT-ACTIVE       VALUE 'A'.
 *         88  PORT-CLOSED       VALUE 'C'.
 *         88  PORT-SUSPENDED    VALUE 'S'.
 * </pre>
 */
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
            if (dbData == null || dbData.isBlank()) {
                return null;
            }
            return PortfolioStatus.fromCode(dbData.charAt(0));
        }
    }
}
