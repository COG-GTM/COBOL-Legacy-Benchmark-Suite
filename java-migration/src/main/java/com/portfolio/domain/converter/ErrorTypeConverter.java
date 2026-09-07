package com.portfolio.domain.converter;
import com.portfolio.domain.ErrorType; import jakarta.persistence.*;
@Converter(autoApply=false) public class ErrorTypeConverter implements AttributeConverter<ErrorType,String>{
 public String convertToDatabaseColumn(ErrorType v){return v==null?null:v.getCode();} public ErrorType convertToEntityAttribute(String v){return v==null?null:ErrorType.fromCode(v.trim());}
}
