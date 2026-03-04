package com.cobolbenchmark.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Return Code Manager - migrated from RTNCODE.cpy.
 * Tracks highest return code and provides classification.
 */
public class ReturnCodeManager {

    private static final Logger logger = LoggerFactory.getLogger(ReturnCodeManager.class);

    private int highestReturnCode = 0;
    private int currentReturnCode = 0;
    private String lastOperation;

    public ReturnCodeManager() {
    }

    /**
     * Set the current return code and track highest.
     * From RTNCDE00: SET-CODE function.
     */
    public void setReturnCode(int code, String operation) {
        this.currentReturnCode = code;
        this.lastOperation = operation;
        if (code > highestReturnCode) {
            highestReturnCode = code;
        }
        logger.debug("Return code {} set for operation: {}", code, operation);
    }

    /**
     * Get the current return code.
     * From RTNCDE00: GET-CODE function.
     */
    public int getCurrentReturnCode() {
        return currentReturnCode;
    }

    /**
     * Get the highest return code encountered.
     */
    public int getHighestReturnCode() {
        return highestReturnCode;
    }

    /**
     * Classify the current return code.
     * From RTNCDE00: 0=SUCCESS, 1-4=WARNING, 5-8=ERROR, other=SEVERE.
     */
    public BatchConstants.ReturnCode classifyCurrentCode() {
        return BatchConstants.ReturnCode.classify(currentReturnCode);
    }

    /**
     * Classify the highest return code.
     */
    public BatchConstants.ReturnCode classifyHighestCode() {
        return BatchConstants.ReturnCode.classify(highestReturnCode);
    }

    /**
     * Log the current return code with classification.
     * From RTNCDE00: LOG-CODE function.
     */
    public void logReturnCode() {
        BatchConstants.ReturnCode classification = classifyCurrentCode();
        logger.info("Return code: {} Classification: {} Operation: {}",
                currentReturnCode, classification, lastOperation);
    }

    /**
     * Reset the return code tracker.
     * From RTNCDE00: INITIALIZE function.
     */
    public void reset() {
        this.highestReturnCode = 0;
        this.currentReturnCode = 0;
        this.lastOperation = null;
    }

    public String getLastOperation() {
        return lastOperation;
    }
}
