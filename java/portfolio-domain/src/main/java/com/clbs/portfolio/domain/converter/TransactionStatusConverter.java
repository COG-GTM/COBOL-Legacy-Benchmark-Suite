package com.clbs.portfolio.domain.converter;

import com.clbs.portfolio.domain.enums.TransactionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {
    @Override
    public String convertToDatabaseColumn(TransactionStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TransactionStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TransactionStatus.fromCode(dbData);
    }
}
