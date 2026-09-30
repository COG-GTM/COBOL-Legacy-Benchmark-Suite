package com.portfolio.domain.converter;

import com.portfolio.domain.AuditType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AuditTypeConverter implements AttributeConverter<AuditType, String> {
  public String convertToDatabaseColumn(AuditType auditType) {
    return auditType == null ? null : auditType.getCode();
  }

  public AuditType convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : AuditType.fromCode(databaseValue.trim());
  }
}
