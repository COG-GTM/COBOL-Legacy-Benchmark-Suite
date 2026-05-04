package com.portfolio.domain;

import java.time.LocalDateTime;

/**
 * Return Code management - migrated from COBOL RTNCODE.cpy.
 */
public class ReturnCodeInfo {

    private char requestType;
    private String programId;
    private int currentCode;
    private int highestCode;
    private int newCode;
    private char status;
    private String message;
    private int responseCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalCodes;
    private int maxCode;
    private int minCode;
    private int returnValue;
    private int highestReturn;
    private char returnStatus;

    public ReturnCodeInfo() {
        this.currentCode = 0;
        this.highestCode = 0;
    }

    public void updateHighest(int code) {
        if (code > highestCode) {
            highestCode = code;
        }
        totalCodes++;
        if (code > maxCode) maxCode = code;
        if (code < minCode || totalCodes == 1) minCode = code;
    }

    public char getRequestType() { return requestType; }
    public void setRequestType(char requestType) { this.requestType = requestType; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public int getCurrentCode() { return currentCode; }
    public void setCurrentCode(int currentCode) { this.currentCode = currentCode; }
    public int getHighestCode() { return highestCode; }
    public void setHighestCode(int highestCode) { this.highestCode = highestCode; }
    public int getNewCode() { return newCode; }
    public void setNewCode(int newCode) { this.newCode = newCode; }
    public char getStatus() { return status; }
    public void setStatus(char status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getResponseCode() { return responseCode; }
    public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public int getTotalCodes() { return totalCodes; }
    public int getMaxCode() { return maxCode; }
    public int getMinCode() { return minCode; }
    public int getReturnValue() { return returnValue; }
    public void setReturnValue(int returnValue) { this.returnValue = returnValue; }
    public int getHighestReturn() { return highestReturn; }
    public void setHighestReturn(int highestReturn) { this.highestReturn = highestReturn; }
    public char getReturnStatus() { return returnStatus; }
    public void setReturnStatus(char returnStatus) { this.returnStatus = returnStatus; }
}
