package com.portfolio.domain.converter;
import com.portfolio.domain.TransactionType; import jakarta.persistence.*;
@Converter(autoApply=false) public class TransactionTypeConverter implements AttributeConverter<TransactionType,String>{
 public String convertToDatabaseColumn(TransactionType v){return v==null?null:v.getCode();} public TransactionType convertToEntityAttribute(String v){return v==null?null:TransactionType.fromCode(v.trim());}
}
