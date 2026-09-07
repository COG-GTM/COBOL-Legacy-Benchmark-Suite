package com.portfolio.domain.converter;
import com.portfolio.domain.ErrorSeverity; import jakarta.persistence.*;
@Converter(autoApply=false) public class ErrorSeverityConverter implements AttributeConverter<ErrorSeverity,Integer>{
 public Integer convertToDatabaseColumn(ErrorSeverity v){return v==null?null:v.getCode();} public ErrorSeverity convertToEntityAttribute(Integer v){return v==null?null:ErrorSeverity.fromCode(v);}
}
