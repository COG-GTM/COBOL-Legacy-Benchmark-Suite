package com.portfolio.portmstr.service;

import com.portfolio.portmstr.model.ErrorLog;
import com.portfolio.portmstr.repository.ErrorLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Error logging service.
 * Replaces COBOL CALL 'ERRPROC' USING ERR-MESSAGE
 * and DB2 INSERT into ERRLOG table.
 */
@Service
public class ErrorLoggingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLoggingService.class);

    private final ErrorLogRepository errorLogRepository;

    public ErrorLoggingService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    public void logError(String programId, char errorType, int severity,
                         String errorCode, String errorMessage, String userId) {
        logError(programId, errorType, severity, errorCode, errorMessage, userId, null);
    }

    public void logError(String programId, char errorType, int severity,
                         String errorCode, String errorMessage, String userId,
                         String additionalInfo) {
        LocalDateTime now = LocalDateTime.now();

        ErrorLog errorLog = new ErrorLog();
        errorLog.setErrorTimestamp(now);
        errorLog.setProgramId(programId);
        errorLog.setErrorType(errorType);
        errorLog.setErrorSeverity(severity);
        errorLog.setErrorCode(errorCode);
        errorLog.setErrorMessage(errorMessage);
        errorLog.setProcessDate(now.toLocalDate());
        errorLog.setProcessTime(now.toLocalTime());
        errorLog.setUserId(userId);
        errorLog.setAdditionalInfo(additionalInfo);

        errorLogRepository.save(errorLog);

        log.error("ERRPROC: Program={}, Type={}, Severity={}, Code={}, Message={}",
                programId, errorType, severity, errorCode, errorMessage);
    }

    public void cleanupOldErrors(int retentionDays) {
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        errorLogRepository.deleteByProcessDateBefore(cutoffDate);
        log.info("Cleaned up error logs before {}", cutoffDate);
    }
}
