package com.portfolio.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the 88-level conditions from PORTFLIO.cpy:
 * <pre>
 *     10  PORT-CLIENT-TYPE    PIC X(1).
 *         88  PORT-INDIVIDUAL    VALUE 'I'.
 *         88  PORT-CORPORATE     VALUE 'C'.
 *         88  PORT-TRUST         VALUE 'T'.
 * </pre>
 */
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

    /**
     * JPA converter so the single-char COBOL code is stored in the database
     * exactly as it was in the original VSAM/DB2 layout.
     */
    @Converter(autoApply = true)
    public static class ClientTypeConverter implements AttributeConverter<ClientType, String> {

        @Override
        public String convertToDatabaseColumn(ClientType attribute) {
            return attribute == null ? null : String.valueOf(attribute.getCode());
        }

        @Override
        public ClientType convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return null;
            }
            return ClientType.fromCode(dbData.charAt(0));
        }
    }
}
