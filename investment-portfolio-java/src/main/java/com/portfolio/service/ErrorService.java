package com.portfolio.service;

import com.portfolio.entity.ErrorCategory;
import com.portfolio.entity.ErrorLog;
import com.portfolio.entity.ErrorSeverity;
import com.portfolio.entity.ErrorType;
import com.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ErrorService {

    private static final Logger log = LoggerFactory.getLogger(ErrorService.class);

    private final ErrorLogRepository errorLogRepository;

    public ErrorService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional
    public int processError(String programId, ErrorCategory category, String errorCode,
                            ErrorSeverity severity, String errorText, String details) {
        LocalDateTime now = LocalDateTime.now();

        ErrorLog errorLog = new ErrorLog();
        errorLog.setErrorTimestamp(now);
        errorLog.setProgramId(programId);
        errorLog.setErrorType(mapCategoryToType(category));
        errorLog.setErrorSeverity(severity.getCode());
        errorLog.setErrorCode(errorCode);
        errorLog.setErrorMessage(truncate(errorText, 200));
        errorLog.setProcessDate(now.toLocalDate());
        errorLog.setProcessTime(now.toLocalTime());
        errorLog.setUserId("SYSTEM");
        errorLog.setAdditionalInfo(truncate(details, 500));

        errorLogRepository.save(errorLog);

        switch (severity) {
            case SUCCESS -> log.info("Program: {} Code: {} - {}", programId, errorCode, errorText);
            case WARNING -> log.warn("Program: {} Code: {} - {}", programId, errorCode, errorText);
            case ERROR, SEVERE -> log.error("Program: {} Code: {} - {}", programId, errorCode, errorText);
            case TERMINAL -> log.error("TERMINAL ERROR - Program: {} Code: {} - {}", programId, errorCode, errorText);
        }

        return severity.getCode();
    }

    @Transactional
    public void cleanup(int retentionDays) {
        LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
        int deleted = errorLogRepository.deleteByProcessDateBefore(cutoff);
        log.info("Error log cleanup: deleted {} records older than {} days", deleted, retentionDays);
    }

    private ErrorType mapCategoryToType(ErrorCategory category) {
        return switch (category) {
            case VSAM, SYSTEM -> ErrorType.SYSTEM;
            case VALIDATION -> ErrorType.DATA;
            case PROCESSING -> ErrorType.APPLICATION;
        };
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
