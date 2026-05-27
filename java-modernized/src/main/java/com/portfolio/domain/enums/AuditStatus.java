package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum AuditStatus {
    SUCCESS("SUCC"),
    FAILURE("FAIL"),
    WARNING("WARN");

    private final String code;

    AuditStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AuditStatus fromCode(String code) {
        for (AuditStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown AuditStatus code: " + code);
    }

    @Converter(autoApply = true)
    public static class AuditStatusConverter implements AttributeConverter<AuditStatus, String> {

        @Override
        public String convertToDatabaseColumn(AuditStatus attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public AuditStatus convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : AuditStatus.fromCode(dbData.trim());
        }
    }
}
