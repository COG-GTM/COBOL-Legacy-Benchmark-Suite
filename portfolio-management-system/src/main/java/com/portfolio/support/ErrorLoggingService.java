package com.portfolio.support;

import com.portfolio.model.ErrorRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Error Logging Service.
 * Migrated from COBOL ERRPROC program.
 * Writes error records to ERRLOG DB2 table (replacing sequential error log file).
 */
@Service
public class ErrorLoggingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLoggingService.class);

    private final ErrorRecordRepository errorRecordRepository;

    public ErrorLoggingService(ErrorRecordRepository errorRecordRepository) {
        this.errorRecordRepository = errorRecordRepository;
    }

    /**
     * Log an error to the ERRLOG table.
     * Mirrors COBOL ERRPROC 2000-PROCESS-ERROR logic.
     */
    public void logError(String programId, String errorType, int severity,
                         String errorCode, String message, String details, String userId) {
        ErrorRecord record = new ErrorRecord();
        record.setErrorTimestamp(LocalDateTime.now());
        record.setProgramId(programId);
        record.setErrorType(errorType);
        record.setErrorSeverity(severity);
        record.setErrorCode(errorCode);
        record.setErrorMessage(message);
        record.setProcessDate(LocalDate.now());
        record.setProcessTime(LocalTime.now());
        record.setUserId(userId);
        record.setAdditionalInfo(details);

        try {
            errorRecordRepository.save(record);
        } catch (Exception ex) {
            // Fallback to console logging if DB write fails
            // (mirrors COBOL ERRPROC 2200-DISPLAY-ERROR)
            log.error("Failed to write error log to DB: {}", ex.getMessage());
        }

        // Always log to console (mirrors COBOL ERRPROC 2200-DISPLAY-ERROR)
        log.error("====================================================");
        log.error("ERROR DETECTED: {}", LocalDateTime.now());
        log.error("PROGRAM:       {}", programId);
        log.error("CATEGORY:      {}", errorType);
        log.error("CODE:          {}", errorCode);
        log.error("SEVERITY:      {}", severity);
        log.error("MESSAGE:       {}", message);
        log.error("DETAILS:       {}", details);
        log.error("====================================================");
    }

    /**
     * Log a warning (RC 4 equivalent).
     */
    public void logWarning(String programId, String errorCode, String message, String userId) {
        logError(programId, ErrorRecord.TYPE_APPLICATION, ErrorRecord.SEVERITY_WARNING,
                errorCode, message, null, userId);
    }

    /**
     * Log an application error (RC 8 equivalent).
     */
    public void logApplicationError(String programId, String errorCode, String message,
                                     String details, String userId) {
        logError(programId, ErrorRecord.TYPE_APPLICATION, ErrorRecord.SEVERITY_ERROR,
                errorCode, message, details, userId);
    }

    /**
     * Log a severe error (RC 12 equivalent).
     */
    public void logSevereError(String programId, String errorCode, String message,
                                String details, String userId) {
        logError(programId, ErrorRecord.TYPE_APPLICATION, ErrorRecord.SEVERITY_SEVERE,
                errorCode, message, details, userId);
    }
}
