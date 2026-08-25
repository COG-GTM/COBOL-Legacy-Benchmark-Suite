package com.portfolio.common;

import com.portfolio.domain.ErrorLog;
import com.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Migration of {@code src/programs/common/ERRPROC.cbl} (Standard Error
 * Processing Subroutine).
 *
 * <p>ERRPROC accepted a program ID, category, code, severity, text, and details
 * (LS-ERROR-REQUEST), appended a formatted record to the ERRLOG file, displayed
 * it, and returned the severity. Here the sequential ERRLOG file becomes the
 * ERRLOG table ({@code src/database/db2/ERRLOG.sql}), DISPLAY becomes SLF4J
 * logging, and the returned severity feeds job-level error counting.
 *
 * <p>Errors are logged in a new transaction (REQUIRES_NEW) so the log record
 * survives a rollback of the failing unit of work — matching the COBOL
 * behavior where ERRLOG was a separate file untouched by DB2 ROLLBACK.
 */
@Service
public class ErrorHandlingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingService.class);

    private final ErrorLogRepository errorLogRepository;

    public ErrorHandlingService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    /**
     * Logs an error (ERRPROC 2000-PROCESS-ERROR) and returns the severity,
     * mirroring {@code MOVE LS-SEVERITY TO LS-RETURN-CODE}.
     *
     * @param programId  LS-PROGRAM-ID PIC X(8)
     * @param errorType  ERR-CATEGORY mapped to ERRLOG.ERROR_TYPE — S=System,
     *                   D=Database, V=Validation, P=Processing
     * @param severity   LS-SEVERITY (ERRLOG.ERROR_SEVERITY 1-4)
     * @param errorCode  LS-ERROR-CODE
     * @param message    LS-ERROR-TEXT PIC X(80)
     * @param details    LS-ERROR-DETAILS PIC X(256), may be null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int logError(String programId, String errorType, int severity,
                        String errorCode, String message, String details) {
        LocalDateTime now = LocalDateTime.now();

        ErrorLog entry = new ErrorLog();
        entry.setKey(new ErrorLog.Key(now, programId));
        entry.setErrorType(errorType);
        entry.setErrorSeverity(severity);
        entry.setErrorCode(errorCode);
        entry.setErrorMessage(truncate(message, 200));
        entry.setProcessDate(now.toLocalDate());
        entry.setProcessTime(now.toLocalTime());
        entry.setUserId(System.getProperty("user.name", "BATCH"));
        entry.setAdditionalInfo(truncate(details, 500));
        errorLogRepository.save(entry);

        log.error("ERROR DETECTED program={} type={} code={} severity={} message={} details={}",
                programId, errorType, errorCode, severity, message, details);

        return severity;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
