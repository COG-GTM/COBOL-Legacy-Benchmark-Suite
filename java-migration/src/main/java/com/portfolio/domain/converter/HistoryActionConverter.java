package com.portfolio.domain.converter;

import com.portfolio.domain.HistoryAction;
import jakarta.persistence.*;

@Converter(autoApply = false)
public class HistoryActionConverter implements AttributeConverter<HistoryAction, String> {
  public String convertToDatabaseColumn(HistoryAction historyAction) {
    return historyAction == null ? null : historyAction.getCode();
  }

  public HistoryAction convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : HistoryAction.fromCode(databaseValue.trim());
  }
}
