package com.portfolio.domain.converter;
import com.portfolio.domain.HistoryAction; import jakarta.persistence.*;
@Converter(autoApply=false) public class HistoryActionConverter implements AttributeConverter<HistoryAction,String>{
 public String convertToDatabaseColumn(HistoryAction v){return v==null?null:v.getCode();} public HistoryAction convertToEntityAttribute(String v){return v==null?null:HistoryAction.fromCode(v.trim());}
}
