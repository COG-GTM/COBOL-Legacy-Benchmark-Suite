package com.portfolio.domain;
public enum PositionStatus { ACTIVE("A"), CLOSED("C"), PENDING("P");
    private final String code; PositionStatus(String code){this.code=code;} public String getCode(){return code;}
    public static PositionStatus fromCode(String code){for(var v:values())if(v.code.equals(code))return v;throw new IllegalArgumentException("Unknown position status: "+code);}
}
