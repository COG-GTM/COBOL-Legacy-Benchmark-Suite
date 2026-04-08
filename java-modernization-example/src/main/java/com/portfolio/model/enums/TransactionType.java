package com.portfolio.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the 88-level conditions from TRNREC.cpy:
 * <pre>
 *     10  TRN-TYPE           PIC X(02).
 *         88  TRN-TYPE-BUY     VALUE 'BU'.
 *         88  TRN-TYPE-SELL    VALUE 'SL'.
 *         88  TRN-TYPE-TRANS   VALUE 'TR'.
 *         88  TRN-TYPE-FEE     VALUE 'FE'.
 * </pre>
 */
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
            if (dbData == null || dbData.isBlank()) {
                return null;
            }
            return TransactionType.fromCode(dbData);
        }
    }
}
