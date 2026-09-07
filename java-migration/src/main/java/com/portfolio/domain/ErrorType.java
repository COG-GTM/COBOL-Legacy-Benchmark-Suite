package com.portfolio.domain;
public enum ErrorType { SYSTEM("S"), APPLICATION("A"), DATA("D");
    private final String code; ErrorType(String code){this.code=code;} public String getCode(){return code;}
    public static ErrorType fromCode(String code){for(var v:values())if(v.code.equals(code))return v;throw new IllegalArgumentException("Unknown error type: "+code);}
}
