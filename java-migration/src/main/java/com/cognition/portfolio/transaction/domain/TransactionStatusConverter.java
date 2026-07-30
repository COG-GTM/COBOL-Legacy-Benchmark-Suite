package com.cognition.portfolio.transaction.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link TransactionStatus} using the literal one-character COBOL value ({@code P},
 * {@code D}, {@code F}, {@code R}).
 */
@Converter
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {

  @Override
  public String convertToDatabaseColumn(TransactionStatus attribute) {
    return attribute == null ? null : attribute.getCode();
  }

  @Override
  public TransactionStatus convertToEntityAttribute(String dbData) {
    return dbData == null
        ? null
        : TransactionStatus.fromCode(dbData)
            .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Status: " + dbData));
  }
}
