package com.portfolio.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum TransactionStatus {
    PENDING('P'),
    DONE('D'),
    FAILED('F'),
    REVERSED('R');

    private final char code;

    TransactionStatus(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static TransactionStatus fromCode(char code) {
        for (TransactionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TransactionStatus code: " + code);
    }

    @Converter(autoApply = true)
    public static class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {

        @Override
        public String convertToDatabaseColumn(TransactionStatus attribute) {
            return attribute == null ? null : String.valueOf(attribute.getCode());
        }

        @Override
        public TransactionStatus convertToEntityAttribute(String dbData) {
            return dbData == null || dbData.isEmpty() ? null : TransactionStatus.fromCode(dbData.charAt(0));
        }
    }
}
