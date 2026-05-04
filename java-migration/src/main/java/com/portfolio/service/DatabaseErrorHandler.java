package com.portfolio.service;

import com.portfolio.entity.ErrorLog;
import com.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class DatabaseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(DatabaseErrorHandler.class);
    private final ErrorLogRepository errorLogRepository;

    public DatabaseErrorHandler(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional
    public void logError(String programId, String errorType, int severity,
                         String errorCode, String message, String additionalInfo) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setErrorTimestamp(LocalDateTime.now());
        errorLog.setProgramId(programId);
        errorLog.setErrorType(errorType);
        errorLog.setErrorSeverity(severity);
        errorLog.setErrorCode(errorCode);
        errorLog.setErrorMessage(message);
        errorLog.setAdditionalInfo(additionalInfo);
        errorLog.setUserId("SYSTEM");
        errorLogRepository.save(errorLog);
        log.error("Error logged: [{}] {} - {}: {}", severity, programId, errorCode, message);
    }

    public void logSystemError(String programId, String message, Exception ex) {
        logError(programId, "S", 3, "SYS001", message,
                ex != null ? ex.getMessage() : null);
    }

    public void logApplicationError(String programId, String message) {
        logError(programId, "A", 3, "APP001", message, null);
    }

    public void logDataError(String programId, String message, String details) {
        logError(programId, "D", 2, "DAT001", message, details);
    }

    public void logWarning(String programId, String message) {
        logError(programId, "A", 2, "WRN001", message, null);
    }
}
