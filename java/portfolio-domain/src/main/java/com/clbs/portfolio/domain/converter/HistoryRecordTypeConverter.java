package com.clbs.portfolio.domain.converter;

import com.clbs.portfolio.domain.enums.HistoryRecordType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class HistoryRecordTypeConverter implements AttributeConverter<HistoryRecordType, String> {
    @Override
    public String convertToDatabaseColumn(HistoryRecordType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public HistoryRecordType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HistoryRecordType.fromCode(dbData);
    }
}
