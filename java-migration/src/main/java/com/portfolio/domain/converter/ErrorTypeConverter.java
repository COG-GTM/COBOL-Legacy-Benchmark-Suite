package com.portfolio.domain.converter;

import com.portfolio.domain.ErrorType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ErrorTypeConverter implements AttributeConverter<ErrorType, String> {
  public String convertToDatabaseColumn(ErrorType errorType) {
    return errorType == null ? null : errorType.getCode();
  }

  public ErrorType convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : ErrorType.fromCode(databaseValue.trim());
  }
}
