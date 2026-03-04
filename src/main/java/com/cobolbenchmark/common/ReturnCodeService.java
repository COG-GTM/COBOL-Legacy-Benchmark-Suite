package com.cobolbenchmark.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Return Code Service - migrated from RTNCDE00.cbl.
 * Operations: INITIALIZE, SET-CODE, GET-CODE, LOG-CODE, ANALYZE.
 * Classification: 0=SUCCESS, 1-4=WARNING, 5-8=ERROR, other=SEVERE.
 */
@Service
public class ReturnCodeService {

    private static final Logger logger = LoggerFactory.getLogger(ReturnCodeService.class);

    private final ReturnCodeManager manager = new ReturnCodeManager();

    /**
     * INITIALIZE operation - reset all return code tracking.
     */
    public void initialize() {
        manager.reset();
        logger.debug("Return code service initialized");
    }

    /**
     * SET-CODE operation - set return code for an operation.
     */
    public void setCode(int code, String operation) {
        manager.setReturnCode(code, operation);
    }

    /**
     * GET-CODE operation - get current return code.
     */
    public int getCode() {
        return manager.getCurrentReturnCode();
    }

    /**
     * GET highest return code encountered.
     */
    public int getHighestCode() {
        return manager.getHighestReturnCode();
    }

    /**
     * LOG-CODE operation - log the current return code with classification.
     */
    public void logCode() {
        manager.logReturnCode();
    }

    /**
     * ANALYZE operation - analyze return codes and provide classification.
     * From RTNCDE00: 0=SUCCESS, 1-4=WARNING, 5-8=ERROR, other=SEVERE.
     */
    public BatchConstants.ReturnCode analyze() {
        BatchConstants.ReturnCode classification = manager.classifyHighestCode();
        logger.info("Return code analysis - Highest: {} Classification: {}",
                manager.getHighestReturnCode(), classification);
        return classification;
    }

    /**
     * Classify a specific return code value.
     */
    public BatchConstants.ReturnCode classify(int code) {
        return BatchConstants.ReturnCode.classify(code);
    }
}
