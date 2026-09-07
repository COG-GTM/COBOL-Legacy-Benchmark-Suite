package com.portfolio.domain.converter;

import com.portfolio.domain.AuditAction;
import jakarta.persistence.*;

@Converter(autoApply = false)
public class AuditActionConverter implements AttributeConverter<AuditAction, String> {
  public String convertToDatabaseColumn(AuditAction auditAction) {
    return auditAction == null ? null : auditAction.getCode();
  }

  public AuditAction convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : AuditAction.fromCode(databaseValue.trim());
  }
}
