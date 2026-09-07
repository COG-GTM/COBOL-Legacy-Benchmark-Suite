package com.portfolio.domain;
public enum AuditStatus { SUCCESS("SUCC"), FAILURE("FAIL"), WARNING("WARN");
    private final String code; AuditStatus(String code){this.code=code;} public String getCode(){return code;}
    public static AuditStatus fromCode(String code){for(var v:values())if(v.code.equals(code.trim()))return v;throw new IllegalArgumentException("Unknown audit status: "+code);}
}
