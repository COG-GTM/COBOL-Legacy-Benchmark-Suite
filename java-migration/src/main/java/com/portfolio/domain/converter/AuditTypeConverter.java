package com.portfolio.domain.converter;
import com.portfolio.domain.AuditType; import jakarta.persistence.*;
@Converter(autoApply=false) public class AuditTypeConverter implements AttributeConverter<AuditType,String>{
 public String convertToDatabaseColumn(AuditType v){return v==null?null:v.getCode();} public AuditType convertToEntityAttribute(String v){return v==null?null:AuditType.fromCode(v.trim());}
}
