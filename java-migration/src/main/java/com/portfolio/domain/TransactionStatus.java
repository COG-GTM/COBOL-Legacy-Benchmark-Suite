package com.portfolio.domain;
public enum TransactionStatus { PENDING("P"), DONE("D"), FAILED("F"), REVERSED("R");
    private final String code; TransactionStatus(String code){this.code=code;} public String getCode(){return code;}
    public static TransactionStatus fromCode(String code){for(var v:values())if(v.code.equals(code))return v;throw new IllegalArgumentException("Unknown transaction status: "+code);}
}
