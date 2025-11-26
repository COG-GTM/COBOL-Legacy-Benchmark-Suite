package com.portfolio.transaction.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    public ErrorLogService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String program, String errorCode, String message) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setProgram(program);
        errorLog.setErrorCode(errorCode);
        errorLog.setErrorMessage(message);
        errorLog.setTimestamp(LocalDateTime.now());
        errorLog.setCategory("PROC");

        errorLogRepository.save(errorLog);
    }
}
