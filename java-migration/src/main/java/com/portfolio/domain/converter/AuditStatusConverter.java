package com.portfolio.domain.converter;

import com.portfolio.domain.AuditStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AuditStatusConverter implements AttributeConverter<AuditStatus, String> {
  public String convertToDatabaseColumn(AuditStatus auditStatus) {
    return auditStatus == null ? null : auditStatus.getCode();
  }

  public AuditStatus convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : AuditStatus.fromCode(databaseValue.trim());
  }
}
