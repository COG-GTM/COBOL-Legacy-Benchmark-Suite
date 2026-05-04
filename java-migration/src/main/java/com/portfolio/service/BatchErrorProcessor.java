package com.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BatchErrorProcessor {

    private static final Logger log = LoggerFactory.getLogger(BatchErrorProcessor.class);
    private final DatabaseErrorHandler errorHandler;

    public BatchErrorProcessor(DatabaseErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void processError(String programId, String errorCode, String message, Exception ex) {
        log.error("Batch error in {}: {} - {}", programId, errorCode, message, ex);
        errorHandler.logError(programId, "A", 3, errorCode, message,
                ex != null ? ex.getMessage() : null);
    }

    public void processWarning(String programId, String message) {
        log.warn("Batch warning in {}: {}", programId, message);
        errorHandler.logWarning(programId, message);
    }

    public void processDataError(String programId, String message, String recordKey) {
        log.error("Data error in {}: {} for key {}", programId, message, recordKey);
        errorHandler.logDataError(programId, message, "Record key: " + recordKey);
    }
}
