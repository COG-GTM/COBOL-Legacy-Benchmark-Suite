package com.clbs.online;

/**
 * INQCOM.cpy communication area. In the migrated online layer it is the request/response body
 * exchanged with the REST controller instead of a DFHCOMMAREA.
 */
public class InquiryCommArea {

    public static final String FUNC_MENU = "MENU";
    public static final String FUNC_PORTFOLIO = "INQP";
    public static final String FUNC_HISTORY = "INQH";
    public static final String FUNC_EXIT = "EXIT";

    private String function = FUNC_MENU;
    private String accountNo = "";
    private int responseCode;
    private String errorMessage = "";

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
