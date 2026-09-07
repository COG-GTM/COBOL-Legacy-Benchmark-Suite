package com.portfolio.domain.converter;
import com.portfolio.domain.ClientType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
@Converter(autoApply=false) public class ClientTypeConverter implements AttributeConverter<ClientType,String> {
    public String convertToDatabaseColumn(ClientType v){return v==null?null:v.getCode();}
    public ClientType convertToEntityAttribute(String v){return v==null?null:ClientType.fromCode(v.trim());}
}
