package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum AuditAction {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE"),
    INQUIRE("INQUIRE"),
    LOGIN("LOGIN"),
    LOGOUT("LOGOUT"),
    STARTUP("STARTUP"),
    SHUTDOWN("SHUTDOWN");

    private final String code;

    AuditAction(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AuditAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (AuditAction action : values()) {
            if (action.code.equals(trimmed)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown AuditAction code: " + code);
    }

    @Converter(autoApply = true)
    public static class AuditActionConverter implements AttributeConverter<AuditAction, String> {

        @Override
        public String convertToDatabaseColumn(AuditAction attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public AuditAction convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : AuditAction.fromCode(dbData);
        }
    }
}
