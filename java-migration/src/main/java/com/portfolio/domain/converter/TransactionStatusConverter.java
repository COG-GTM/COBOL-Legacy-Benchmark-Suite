package com.portfolio.domain.converter;

import com.portfolio.domain.TransactionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {
  public String convertToDatabaseColumn(TransactionStatus transactionStatus) {
    return transactionStatus == null ? null : transactionStatus.getCode();
  }

  public TransactionStatus convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : TransactionStatus.fromCode(databaseValue.trim());
  }
}
