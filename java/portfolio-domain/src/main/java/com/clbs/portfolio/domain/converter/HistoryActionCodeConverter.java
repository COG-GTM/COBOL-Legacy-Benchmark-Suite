package com.clbs.portfolio.domain.converter;

import com.clbs.portfolio.domain.enums.HistoryActionCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class HistoryActionCodeConverter implements AttributeConverter<HistoryActionCode, String> {
    @Override
    public String convertToDatabaseColumn(HistoryActionCode attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public HistoryActionCode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HistoryActionCode.fromCode(dbData);
    }
}
