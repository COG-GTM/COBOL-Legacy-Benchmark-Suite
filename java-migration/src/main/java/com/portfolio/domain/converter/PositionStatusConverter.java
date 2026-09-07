package com.portfolio.domain.converter;
import com.portfolio.domain.PositionStatus; import jakarta.persistence.*;
@Converter(autoApply=false) public class PositionStatusConverter implements AttributeConverter<PositionStatus,String>{
 public String convertToDatabaseColumn(PositionStatus v){return v==null?null:v.getCode();} public PositionStatus convertToEntityAttribute(String v){return v==null?null:PositionStatus.fromCode(v.trim());}
}
