package com.portfolio.service.common;

import com.portfolio.model.entity.ErrorLogEntry;
import com.portfolio.model.enums.ErrorSeverity;
import com.portfolio.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ErrorProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorProcessingService.class);

    private final ErrorLogRepository errorLogRepository;

    public ErrorProcessingService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional
    public int processError(String programId, String category, String code,
                            int severity, String text, String details) {
        String message = buildErrorMessage(programId, category, code, text);
        writeLog(programId, category, severity, code, message, details);
        displayError(severity, message);
        return severity;
    }

    private String buildErrorMessage(String programId, String category, String code, String text) {
        return String.format("[%s] %s-%s: %s", programId, category, code, text);
    }

    private void writeLog(String programId, String category, int severity,
                          String code, String message, String details) {
        ErrorLogEntry entry = new ErrorLogEntry();
        entry.setErrorTimestamp(LocalDateTime.now());
        entry.setProgramId(programId);
        entry.setErrorType(category != null && !category.isEmpty() ? category.charAt(0) : 'A');
        entry.setErrorSeverity(severity);
        entry.setErrorCode(code);
        entry.setErrorMessage(message.length() > 200 ? message.substring(0, 200) : message);
        entry.setProcessDate(LocalDate.now());
        entry.setProcessTime(LocalTime.now());
        entry.setUserId("SYSTEM");
        entry.setAdditionalInfo(details);
        errorLogRepository.save(entry);
    }

    private void displayError(int severity, String message) {
        if (severity >= ErrorSeverity.SEVERE.getLevel()) {
            log.error(message);
        } else if (severity >= ErrorSeverity.ERROR.getLevel()) {
            log.error(message);
        } else if (severity >= ErrorSeverity.WARNING.getLevel()) {
            log.warn(message);
        } else {
            log.info(message);
        }
    }
}
