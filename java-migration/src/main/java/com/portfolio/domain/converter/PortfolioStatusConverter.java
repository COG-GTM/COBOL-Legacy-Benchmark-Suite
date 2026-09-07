package com.portfolio.domain.converter;
import com.portfolio.domain.PortfolioStatus; import jakarta.persistence.*;
@Converter(autoApply=false) public class PortfolioStatusConverter implements AttributeConverter<PortfolioStatus,String>{
 public String convertToDatabaseColumn(PortfolioStatus v){return v==null?null:v.getCode();} public PortfolioStatus convertToEntityAttribute(String v){return v==null?null:PortfolioStatus.fromCode(v.trim());}
}
