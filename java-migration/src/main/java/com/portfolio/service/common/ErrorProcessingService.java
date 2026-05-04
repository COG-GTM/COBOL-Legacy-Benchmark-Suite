package com.portfolio.service.common;

import com.portfolio.domain.ErrorInfo;
import com.portfolio.domain.ErrorLogRecord;
import com.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Error Processing Service - migrated from COBOL ERRPROC.cbl.
 * Replaces batch error handler: logs errors, sets return codes.
 * In Java: throw/catch custom exceptions, log via SLF4J.
 */
@Service
public class ErrorProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorProcessingService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ErrorLogRepository errorLogRepository;

    public ErrorProcessingService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional
    public int processError(ErrorInfo errorInfo) {
        logToConsole(errorInfo);
        writeToDatabase(errorInfo);
        return errorInfo.getSeverity();
    }

    @Transactional
    public int processError(String programId, String category, String errorCode,
                            int severity, String text, String details) {
        ErrorInfo info = new ErrorInfo(programId, category, errorCode, severity, text, details);
        return processError(info);
    }

    private void logToConsole(ErrorInfo errorInfo) {
        String msg = String.format("ERROR [%s] Program=%s Category=%s Code=%s Severity=%d: %s | %s",
                errorInfo.getTimestamp(), errorInfo.getProgram(), errorInfo.getCategory(),
                errorInfo.getCode(), errorInfo.getSeverity(), errorInfo.getText(),
                errorInfo.getDetails());

        if (errorInfo.getSeverity() >= 3) {
            log.error(msg);
        } else if (errorInfo.getSeverity() == 2) {
            log.warn(msg);
        } else {
            log.info(msg);
        }
    }

    private void writeToDatabase(ErrorInfo errorInfo) {
        try {
            ErrorLogRecord record = new ErrorLogRecord();
            record.setErrorTimestamp(errorInfo.getTimestamp() != null
                    ? errorInfo.getTimestamp() : LocalDateTime.now());
            record.setProgramId(truncate(errorInfo.getProgram(), 8));
            record.setErrorType(truncate(errorInfo.getCategory(), 1));
            record.setErrorSeverity(errorInfo.getSeverity());
            record.setErrorCode(truncate(errorInfo.getCode(), 8));
            record.setErrorMessage(truncate(errorInfo.getText(), 200));
            record.setProcessDate(LocalDate.now());
            record.setProcessTime(LocalDateTime.now().format(TIME_FMT));
            record.setUserId("SYSTEM");
            record.setAdditionalInfo(truncate(errorInfo.getDetails(), 500));

            errorLogRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to write error log to database", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
