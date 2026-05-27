package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum TransactionType {
    BUY("BU"),
    SELL("SL"),
    TRANSFER("TR"),
    FEE("FE");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TransactionType code: " + code);
    }

    @Converter(autoApply = true)
    public static class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {

        @Override
        public String convertToDatabaseColumn(TransactionType attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public TransactionType convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : TransactionType.fromCode(dbData.trim());
        }
    }
}
