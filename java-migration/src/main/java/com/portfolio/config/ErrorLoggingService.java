package com.portfolio.config;

import com.portfolio.domain.*;
import com.portfolio.repository.ErrorLogRepository;
import java.time.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErrorLoggingService {
  private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingService.class);
  private final ErrorLogRepository errorLogRepository;

  public ErrorLoggingService(ErrorLogRepository errorLogRepository) {
    this.errorLogRepository = errorLogRepository;
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void log(
      String program,
      ErrorType errorType,
      ErrorSeverity severity,
      String code,
      String message,
      String additionalInfo) {
    ErrorLog errorLog = new ErrorLog();
    LocalDateTime now = LocalDateTime.now();
    errorLog.setErrorTimestamp(now);
    errorLog.setProgramId(program);
    errorLog.setErrorType(errorType);
    errorLog.setErrorSeverity(severity);
    errorLog.setErrorCode(code);
    errorLog.setErrorMessage(message);
    errorLog.setProcessDate(now.toLocalDate());
    errorLog.setProcessTime(now.toLocalTime());
    errorLog.setUserId("SYSTEM");
    errorLog.setAdditionalInfo(additionalInfo);
    errorLogRepository.save(errorLog);
    switch (severity) {
      case INFO -> logger.info("{} {}: {}", program, code, message);
      case WARNING -> logger.warn("{} {}: {}", program, code, message);
      case ERROR, SEVERE -> logger.error("{} {}: {}", program, code, message);
    }
  }

  public String determineAction(ErrorSeverity severity) {
    return severity == ErrorSeverity.SEVERE
        ? "ABEND"
        : (severity == ErrorSeverity.WARNING || severity == ErrorSeverity.INFO
            ? "CONTINUE"
            : "RETURN");
  }
}
