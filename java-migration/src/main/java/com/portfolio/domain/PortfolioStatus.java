package com.portfolio.domain;
public enum PortfolioStatus { ACTIVE("A"), CLOSED("C"), SUSPENDED("S");
    private final String code; PortfolioStatus(String code){this.code=code;} public String getCode(){return code;}
    public static PortfolioStatus fromCode(String code){for(var v:values())if(v.code.equals(code))return v;throw new IllegalArgumentException("Unknown portfolio status: "+code);}
}
