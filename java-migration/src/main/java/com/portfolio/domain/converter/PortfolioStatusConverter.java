package com.portfolio.domain.converter;

import com.portfolio.domain.PortfolioStatus;
import jakarta.persistence.*;

@Converter(autoApply = false)
public class PortfolioStatusConverter implements AttributeConverter<PortfolioStatus, String> {
  public String convertToDatabaseColumn(PortfolioStatus portfolioStatus) {
    return portfolioStatus == null ? null : portfolioStatus.getCode();
  }

  public PortfolioStatus convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : PortfolioStatus.fromCode(databaseValue.trim());
  }
}
