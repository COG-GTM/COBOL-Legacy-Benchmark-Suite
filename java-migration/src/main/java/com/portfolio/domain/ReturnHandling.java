package com.portfolio.domain;

import com.portfolio.domain.enums.ActionFlag;
import com.portfolio.domain.enums.ErrorType;
import com.portfolio.domain.enums.ReturnCode;

/**
 * Return handling structure - migrated from COBOL RETHND.cpy.
 */
public class ReturnHandling {

    private ReturnCode returnCode;
    private int reasonCode;
    private String moduleId;
    private String functionId;
    private String programName;
    private String paragraphName;
    private String errorRoutine;
    private ErrorType errorType;
    private String errorCode;
    private String errorText;
    private String systemCode;
    private String systemMsg;
    private ActionFlag actionFlag;
    private int retryCount;
    private int maxRetries;

    public ReturnHandling() {
        this.returnCode = ReturnCode.SUCCESS;
        this.actionFlag = ActionFlag.CONTINUE;
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    public ReturnCode getReturnCode() { return returnCode; }
    public void setReturnCode(ReturnCode returnCode) { this.returnCode = returnCode; }
    public int getReasonCode() { return reasonCode; }
    public void setReasonCode(int reasonCode) { this.reasonCode = reasonCode; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getFunctionId() { return functionId; }
    public void setFunctionId(String functionId) { this.functionId = functionId; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    public String getParagraphName() { return paragraphName; }
    public void setParagraphName(String paragraphName) { this.paragraphName = paragraphName; }
    public String getErrorRoutine() { return errorRoutine; }
    public void setErrorRoutine(String errorRoutine) { this.errorRoutine = errorRoutine; }
    public ErrorType getErrorType() { return errorType; }
    public void setErrorType(ErrorType errorType) { this.errorType = errorType; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorText() { return errorText; }
    public void setErrorText(String errorText) { this.errorText = errorText; }
    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }
    public String getSystemMsg() { return systemMsg; }
    public void setSystemMsg(String systemMsg) { this.systemMsg = systemMsg; }
    public ActionFlag getActionFlag() { return actionFlag; }
    public void setActionFlag(ActionFlag actionFlag) { this.actionFlag = actionFlag; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public boolean canRetry() {
        return actionFlag == ActionFlag.RETRY && retryCount < maxRetries;
    }
}
