package com.portfolio.common;
public enum VsamStatus {
    SUCCESS("00","Success"), DUPLICATE("22","Duplicate key"), NOT_FOUND("23","Record not found"), END_OF_FILE("10","End of file");
    private final String code; private final String message;
    VsamStatus(String code,String message){this.code=code;this.message=message;} public String getCode(){return code;} public String getMessage(){return message;}
}
