package com.portfolio.domain.converter;

import com.portfolio.domain.ClientType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ClientTypeConverter implements AttributeConverter<ClientType, String> {
  public String convertToDatabaseColumn(ClientType clientType) {
    return clientType == null ? null : clientType.getCode();
  }

  public ClientType convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : ClientType.fromCode(databaseValue.trim());
  }
}
