package com.portfolio.domain.converter;
import com.portfolio.domain.AuditStatus; import jakarta.persistence.*;
@Converter(autoApply=false) public class AuditStatusConverter implements AttributeConverter<AuditStatus,String>{
 public String convertToDatabaseColumn(AuditStatus v){return v==null?null:v.getCode();} public AuditStatus convertToEntityAttribute(String v){return v==null?null:AuditStatus.fromCode(v.trim());}
}
