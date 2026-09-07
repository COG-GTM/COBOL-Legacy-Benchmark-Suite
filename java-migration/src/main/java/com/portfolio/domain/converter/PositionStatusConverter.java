package com.portfolio.domain.converter;

import com.portfolio.domain.PositionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PositionStatusConverter implements AttributeConverter<PositionStatus, String> {
  public String convertToDatabaseColumn(PositionStatus positionStatus) {
    return positionStatus == null ? null : positionStatus.getCode();
  }

  public PositionStatus convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : PositionStatus.fromCode(databaseValue.trim());
  }
}
