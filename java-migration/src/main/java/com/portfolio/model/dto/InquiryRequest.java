package com.portfolio.model.dto;

public class InquiryRequest {

    private String function;
    private String accountNo;

    public InquiryRequest() {
    }

    public InquiryRequest(String function, String accountNo) {
        this.function = function;
        this.accountNo = accountNo;
    }

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
}
