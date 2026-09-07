package com.portfolio.domain.converter;
import com.portfolio.domain.HistoryRecordType; import jakarta.persistence.*;
@Converter(autoApply=false) public class HistoryRecordTypeConverter implements AttributeConverter<HistoryRecordType,String>{
 public String convertToDatabaseColumn(HistoryRecordType v){return v==null?null:v.getCode();} public HistoryRecordType convertToEntityAttribute(String v){return v==null?null:HistoryRecordType.fromCode(v.trim());}
}
