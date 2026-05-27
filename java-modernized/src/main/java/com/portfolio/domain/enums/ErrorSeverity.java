package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ErrorSeverity {
    INFO(1),
    WARNING(2),
    ERROR(3),
    SEVERE(4);

    private final int level;

    ErrorSeverity(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static ErrorSeverity fromLevel(int level) {
        for (ErrorSeverity severity : values()) {
            if (severity.level == level) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorSeverity level: " + level);
    }

    @Converter(autoApply = true)
    public static class ErrorSeverityConverter implements AttributeConverter<ErrorSeverity, Integer> {

        @Override
        public Integer convertToDatabaseColumn(ErrorSeverity attribute) {
            return attribute == null ? null : attribute.getLevel();
        }

        @Override
        public ErrorSeverity convertToEntityAttribute(Integer dbData) {
            return dbData == null ? null : ErrorSeverity.fromLevel(dbData);
        }
    }
}
