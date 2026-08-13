package com.ipms.domain;

/**
 * RETURN-CODE-AREA from {@code src/copybook/common/RTNCODE.cpy} — the request/response
 * structure passed to the return-code management service (RTNCDE00).
 */
public class ReturnCodeArea {

    /** RC-REQUEST-TYPE level-88 values (I/S/G/L/A). */
    public enum RequestType {
        INITIALIZE("I"), SET_CODE("S"), GET_CODE("G"), LOG_CODE("L"), ANALYZE("A");

        private final String code;

        RequestType(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    /** RC-STATUS level-88 values (S/W/E/F). */
    public enum Status {
        SUCCESS("S"), WARNING("W"), ERROR("E"), SEVERE("F");

        private final String code;

        Status(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    private RequestType requestType;      // RC-REQUEST-TYPE  PIC X
    private String programId;             // RC-PROGRAM-ID    PIC X(8)
    private int currentCode;              // RC-CURRENT-CODE  PIC S9(4) COMP
    private int highestCode;              // RC-HIGHEST-CODE  PIC S9(4) COMP
    private int newCode;                  // RC-NEW-CODE      PIC S9(4) COMP
    private Status status;                // RC-STATUS        PIC X
    private String message;               // RC-MESSAGE       PIC X(80)
    private long responseCode;            // RC-RESPONSE-CODE PIC S9(8) COMP
    private String startTime;             // RC-START-TIME    PIC X(26)
    private String endTime;               // RC-END-TIME      PIC X(26)
    private long totalCodes;              // RC-TOTAL-CODES   PIC S9(8) COMP
    private int maxCode;                  // RC-MAX-CODE      PIC S9(4) COMP
    private int minCode;                  // RC-MIN-CODE      PIC S9(4) COMP
    private int returnValue;              // RC-RETURN-VALUE  PIC S9(4) COMP
    private int highestReturn;            // RC-HIGHEST-RETURN PIC S9(4) COMP
    private Status returnStatus;          // RC-RETURN-STATUS PIC X

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(long responseCode) {
        this.responseCode = responseCode;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
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

    public int getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(int returnValue) {
        this.returnValue = returnValue;
    }

    public int getHighestReturn() {
        return highestReturn;
    }

    public void setHighestReturn(int highestReturn) {
        this.highestReturn = highestReturn;
    }

    public Status getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(Status returnStatus) {
        this.returnStatus = returnStatus;
    }
}
