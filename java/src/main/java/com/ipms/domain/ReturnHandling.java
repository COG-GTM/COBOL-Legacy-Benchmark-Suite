package com.ipms.domain;

/**
 * RETURN-HANDLING from {@code src/copybook/common/RETHND.cpy} — the standard
 * return-status / error-detail / retry-action structure shared by programs.
 */
public class ReturnHandling {

    /** ERROR-TYPE level-88 values (V/P/D/F/S). */
    public enum ErrorType {
        VALIDATION("V"), PROCESSING("P"), DATABASE("D"), FILE("F"), SECURITY("S");

        private final String code;

        ErrorType(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    /** ACTION-FLAG level-88 values (C/A/R). */
    public enum ActionFlag {
        CONTINUE("C"), ABORT("A"), RETRY("R");

        private final String code;

        ActionFlag(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    public static final int MAX_RETRIES_DEFAULT = 3;

    // RETURN-STATUS
    private int returnCode;               // RETURN-CODE   PIC S9(4) COMP (0/4/8/12/16)
    private int reasonCode;               // REASON-CODE   PIC S9(4) COMP
    private String moduleId;              // MODULE-ID     PIC X(8)
    private String functionId;            // FUNCTION-ID   PIC X(8)

    // RETURN-DETAILS / ERROR-LOCATION
    private String programName;           // PROGRAM-NAME   PIC X(8)
    private String paragraphName;         // PARAGRAPH-NAME PIC X(8)
    private String errorRoutine;          // ERROR-ROUTINE  PIC X(8)

    // RETURN-DETAILS / ERROR-INFO
    private ErrorType errorType;          // ERROR-TYPE PIC X(1)
    private String errorCode;             // ERROR-CODE PIC X(4)
    private String errorText;             // ERROR-TEXT PIC X(80)

    // RETURN-DETAILS / SYSTEM-INFO
    private String systemCode;            // SYSTEM-CODE PIC X(4)
    private String systemMsg;             // SYSTEM-MSG  PIC X(80)

    // RETURN-ACTIONS
    private ActionFlag actionFlag;        // ACTION-FLAG PIC X(1)
    private int retryCount;               // RETRY-COUNT PIC 9(2) COMP
    private int maxRetries = MAX_RETRIES_DEFAULT; // MAX-RETRIES PIC 9(2) COMP VALUE 3

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public int getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(int reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getFunctionId() {
        return functionId;
    }

    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getParagraphName() {
        return paragraphName;
    }

    public void setParagraphName(String paragraphName) {
        this.paragraphName = paragraphName;
    }

    public String getErrorRoutine() {
        return errorRoutine;
    }

    public void setErrorRoutine(String errorRoutine) {
        this.errorRoutine = errorRoutine;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getSystemMsg() {
        return systemMsg;
    }

    public void setSystemMsg(String systemMsg) {
        this.systemMsg = systemMsg;
    }

    public ActionFlag getActionFlag() {
        return actionFlag;
    }

    public void setActionFlag(ActionFlag actionFlag) {
        this.actionFlag = actionFlag;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
