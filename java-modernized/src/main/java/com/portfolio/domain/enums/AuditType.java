package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum AuditType {
    TRANSACTION("TRAN"),
    USER_ACTION("USER"),
    SYSTEM_EVENT("SYST");

    private final String code;

    AuditType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AuditType fromCode(String code) {
        for (AuditType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AuditType code: " + code);
    }

    @Converter(autoApply = true)
    public static class AuditTypeConverter implements AttributeConverter<AuditType, String> {

        @Override
        public String convertToDatabaseColumn(AuditType attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public AuditType convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : AuditType.fromCode(dbData.trim());
        }
    }
}
