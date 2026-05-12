package com.portfolio.model.dto;

public class ReturnCodeArea {

    private char requestType;
    private String programId;
    private int currentCode;
    private int highestCode;
    private int newCode;
    private char status;
    private String message;

    public ReturnCodeArea() {
    }

    public char getRequestType() {
        return requestType;
    }

    public void setRequestType(char requestType) {
        this.requestType = requestType;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public int getCurrentCode() {
        return currentCode;
    }

    public void setCurrentCode(int currentCode) {
        this.currentCode = currentCode;
    }

    public int getHighestCode() {
        return highestCode;
    }

    public void setHighestCode(int highestCode) {
        this.highestCode = highestCode;
    }

    public int getNewCode() {
        return newCode;
    }

    public void setNewCode(int newCode) {
        this.newCode = newCode;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
