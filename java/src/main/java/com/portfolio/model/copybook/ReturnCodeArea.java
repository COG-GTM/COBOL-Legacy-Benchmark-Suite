package com.portfolio.model.copybook;

/**
 * Migrated from copybook {@code src/copybook/common/RTNCODE.cpy} (01 RETURN-CODE-AREA)
 * and {@code src/copybook/common/RETHND.cpy} (01 RETURN-HANDLING).
 *
 * <p>Return-code management area used by RTNCDE00 and callers. Standard code
 * thresholds: 0=Success, 4=Warning, 8=Error, 12=Severe, 16=Critical.
 */
public class ReturnCodeArea {

    /** RC-REQUEST-TYPE PIC X — I=Initialize, S=Set, G=Get, L=Log, A=Analyze (level-88s). */
    private String requestType;

    /** RC-PROGRAM-ID PIC X(8). */
    private String programId;

    /** RC-CURRENT-CODE PIC S9(4) COMP. */
    private int currentCode;

    /** RC-HIGHEST-CODE PIC S9(4) COMP. */
    private int highestCode;

    /** RC-NEW-CODE PIC S9(4) COMP. */
    private int newCode;

    /** RC-STATUS PIC X — S=Success, W=Warning, E=Error, F=Severe (level-88s). */
    private String status;

    /** RC-MESSAGE PIC X(80). */
    private String message;

    /** RC-RESPONSE-CODE PIC S9(8) COMP. */
    private int responseCode;

    /** RC-START-TIME PIC X(26). */
    private String startTime;

    /** RC-END-TIME PIC X(26). */
    private String endTime;

    /** RC-TOTAL-CODES PIC S9(8) COMP. */
    private int totalCodes;

    /** RC-MAX-CODE PIC S9(4) COMP. */
    private int maxCode;

    /** RC-MIN-CODE PIC S9(4) COMP. */
    private int minCode;

    /** RC-RETURN-VALUE PIC S9(4) COMP. */
    private int returnValue;

    /** RC-HIGHEST-RETURN PIC S9(4) COMP. */
    private int highestReturn;

    /** RC-RETURN-STATUS PIC X. */
    private String returnStatus;

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }
    public int getCurrentCode() { return currentCode; }
    public void setCurrentCode(int currentCode) { this.currentCode = currentCode; }
    public int getHighestCode() { return highestCode; }
    public void setHighestCode(int highestCode) { this.highestCode = highestCode; }
    public int getNewCode() { return newCode; }
    public void setNewCode(int newCode) { this.newCode = newCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getResponseCode() { return responseCode; }
    public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public int getTotalCodes() { return totalCodes; }
    public void setTotalCodes(int totalCodes) { this.totalCodes = totalCodes; }
    public int getMaxCode() { return maxCode; }
    public void setMaxCode(int maxCode) { this.maxCode = maxCode; }
    public int getMinCode() { return minCode; }
    public void setMinCode(int minCode) { this.minCode = minCode; }
    public int getReturnValue() { return returnValue; }
    public void setReturnValue(int returnValue) { this.returnValue = returnValue; }
    public int getHighestReturn() { return highestReturn; }
    public void setHighestReturn(int highestReturn) { this.highestReturn = highestReturn; }
    public String getReturnStatus() { return returnStatus; }
    public void setReturnStatus(String returnStatus) { this.returnStatus = returnStatus; }
}
