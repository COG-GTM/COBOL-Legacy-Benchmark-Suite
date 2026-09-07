package com.portfolio.domain.converter;

import com.portfolio.domain.TransactionType;
import jakarta.persistence.*;

@Converter(autoApply = false)
public class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {
  public String convertToDatabaseColumn(TransactionType transactionType) {
    return transactionType == null ? null : transactionType.getCode();
  }

  public TransactionType convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : TransactionType.fromCode(databaseValue.trim());
  }
}
