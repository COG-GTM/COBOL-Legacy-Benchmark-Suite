package com.clbs.portfolio.domain.converter;

import com.clbs.portfolio.domain.enums.PositionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PositionStatusConverter implements AttributeConverter<PositionStatus, String> {
    @Override
    public String convertToDatabaseColumn(PositionStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public PositionStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PositionStatus.fromCode(dbData);
    }
}
