package com.portfolio.domain.converter;

import com.portfolio.domain.ErrorSeverity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ErrorSeverityConverter implements AttributeConverter<ErrorSeverity, Integer> {
  public Integer convertToDatabaseColumn(ErrorSeverity errorSeverity) {
    return errorSeverity == null ? null : errorSeverity.getCode();
  }

  public ErrorSeverity convertToEntityAttribute(Integer databaseValue) {
    return databaseValue == null ? null : ErrorSeverity.fromCode(databaseValue);
  }
}
