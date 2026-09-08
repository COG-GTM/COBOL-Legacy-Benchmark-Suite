package com.clbs.common;

import com.clbs.domain.ReturnCode;

/** RTNCODE.cpy RETURN-CODE-AREA: the state RTNCDE00 keeps for one program. */
public class ReturnCodeArea {

    private String programId = "";
    private int currentCode;
    private int highestCode;
    private ReturnCode.Status status = ReturnCode.Status.SUCCESS;
    private String message = "";
    private int responseCode;
    private long totalCodes;
    private int maxCode;
    private int minCode;

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

    public ReturnCode.Status getStatus() {
        return status;
    }

    public void setStatus(ReturnCode.Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public long getTotalCodes() {
        return totalCodes;
    }

    public void setTotalCodes(long totalCodes) {
        this.totalCodes = totalCodes;
    }

    public int getMaxCode() {
        return maxCode;
    }

    public void setMaxCode(int maxCode) {
        this.maxCode = maxCode;
    }

    public int getMinCode() {
        return minCode;
    }

    public void setMinCode(int minCode) {
        this.minCode = minCode;
    }
}
