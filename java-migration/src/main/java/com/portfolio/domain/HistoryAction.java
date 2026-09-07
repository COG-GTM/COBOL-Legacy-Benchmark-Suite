package com.portfolio.domain;
public enum HistoryAction { ADD("A"), CHANGE("C"), DELETE("D");
    private final String code; HistoryAction(String code){this.code=code;} public String getCode(){return code;}
    public static HistoryAction fromCode(String code){for(var v:values())if(v.code.equals(code))return v;throw new IllegalArgumentException("Unknown history action: "+code);}
}
