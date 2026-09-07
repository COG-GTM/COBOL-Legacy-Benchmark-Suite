package com.portfolio.domain;
public enum AuditType { TRAN("TRAN"), USER("USER"), SYSTEM("SYST");
    private final String code; AuditType(String code){this.code=code;} public String getCode(){return code;}
    public static AuditType fromCode(String code){for(var v:values())if(v.code.equals(code))return v;throw new IllegalArgumentException("Unknown audit type: "+code);}
}
