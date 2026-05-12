package com.portfolio.model.dto;

public class InquiryResponse {

    private int responseCode;
    private String errorMessage;
    private Object data;

    public InquiryResponse() {
    }

    public InquiryResponse(int responseCode, Object data) {
        this.responseCode = responseCode;
        this.data = data;
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
