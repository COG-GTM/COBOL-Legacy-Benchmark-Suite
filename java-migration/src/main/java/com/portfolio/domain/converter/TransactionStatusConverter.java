package com.portfolio.domain.converter;
import com.portfolio.domain.TransactionStatus; import jakarta.persistence.*;
@Converter(autoApply=false) public class TransactionStatusConverter implements AttributeConverter<TransactionStatus,String>{
 public String convertToDatabaseColumn(TransactionStatus v){return v==null?null:v.getCode();} public TransactionStatus convertToEntityAttribute(String v){return v==null?null:TransactionStatus.fromCode(v.trim());}
}
