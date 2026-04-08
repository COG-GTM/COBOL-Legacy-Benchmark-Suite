package com.portfolio.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the 88-level conditions from TRNREC.cpy:
 * <pre>
 *     10  TRN-STATUS        PIC X(01).
 *         88  TRN-STATUS-PEND   VALUE 'P'.
 *         88  TRN-STATUS-DONE   VALUE 'D'.
 *         88  TRN-STATUS-FAIL   VALUE 'F'.
 *         88  TRN-STATUS-REV    VALUE 'R'.
 * </pre>
 */
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
            if (dbData == null || dbData.isBlank()) {
                return null;
            }
            return TransactionStatus.fromCode(dbData.charAt(0));
        }
    }
}
