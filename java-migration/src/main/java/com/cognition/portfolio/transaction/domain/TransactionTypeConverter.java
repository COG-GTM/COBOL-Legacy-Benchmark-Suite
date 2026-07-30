package com.cognition.portfolio.transaction.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link TransactionType} using the literal two-character COBOL value ({@code BU},
 * {@code SL}, {@code TR}, {@code FE}) so the table stays readable next to the VSAM file.
 */
@Converter
public class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {

  @Override
  public String convertToDatabaseColumn(TransactionType attribute) {
    return attribute == null ? null : attribute.getCode();
  }

  @Override
  public TransactionType convertToEntityAttribute(String dbData) {
    return dbData == null
        ? null
        : TransactionType.fromCode(dbData)
            .orElseThrow(() -> new IllegalArgumentException("Invalid Transaction Type: " + dbData));
  }
}
