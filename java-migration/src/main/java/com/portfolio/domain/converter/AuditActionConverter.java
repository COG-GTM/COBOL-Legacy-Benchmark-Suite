package com.portfolio.domain.converter;
import com.portfolio.domain.AuditAction; import jakarta.persistence.*;
@Converter(autoApply=false) public class AuditActionConverter implements AttributeConverter<AuditAction,String>{
 public String convertToDatabaseColumn(AuditAction v){return v==null?null:v.getCode();} public AuditAction convertToEntityAttribute(String v){return v==null?null:AuditAction.fromCode(v.trim());}
}
