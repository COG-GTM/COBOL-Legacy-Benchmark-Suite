package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum HistoryRecordType {
    PORTFOLIO("PT"),
    POSITION("PS"),
    TRANSACTION("TR");

    private final String code;

    HistoryRecordType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static HistoryRecordType fromCode(String code) {
        for (HistoryRecordType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown HistoryRecordType code: " + code);
    }

    @Converter(autoApply = true)
    public static class HistoryRecordTypeConverter implements AttributeConverter<HistoryRecordType, String> {

        @Override
        public String convertToDatabaseColumn(HistoryRecordType attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public HistoryRecordType convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : HistoryRecordType.fromCode(dbData.trim());
        }
    }
}
