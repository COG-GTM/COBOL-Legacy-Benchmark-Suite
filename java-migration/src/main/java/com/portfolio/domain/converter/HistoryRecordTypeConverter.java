package com.portfolio.domain.converter;

import com.portfolio.domain.HistoryRecordType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class HistoryRecordTypeConverter implements AttributeConverter<HistoryRecordType, String> {
  public String convertToDatabaseColumn(HistoryRecordType historyRecordType) {
    return historyRecordType == null ? null : historyRecordType.getCode();
  }

  public HistoryRecordType convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : HistoryRecordType.fromCode(databaseValue.trim());
  }
}
