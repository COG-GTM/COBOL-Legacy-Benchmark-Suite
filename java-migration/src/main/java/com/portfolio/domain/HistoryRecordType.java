package com.portfolio.domain;
public enum HistoryRecordType { PORTFOLIO("PT"), POSITION("PS"), TRANSACTION("TR");
    private final String code; HistoryRecordType(String code){this.code=code;} public String getCode(){return code;}
    public static HistoryRecordType fromCode(String code){for(var v:values())if(v.code.equals(code))return v;throw new IllegalArgumentException("Unknown history record type: "+code);}
}
