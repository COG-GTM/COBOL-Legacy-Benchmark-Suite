package com.portfolio.config;
import com.portfolio.domain.*; import com.portfolio.repository.ErrorLogRepository; import org.slf4j.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.*;
@Service public class ErrorLoggingService {
 private static final Logger log=LoggerFactory.getLogger(ErrorLoggingService.class); private final ErrorLogRepository repository;
 public ErrorLoggingService(ErrorLogRepository repository){this.repository=repository;}
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
 public void log(String program,ErrorType type,ErrorSeverity severity,String code,String message,String additionalInfo){
  ErrorLog e=new ErrorLog(); LocalDateTime now=LocalDateTime.now(); e.setErrorTimestamp(now);e.setProgramId(program);e.setErrorType(type);e.setErrorSeverity(severity);e.setErrorCode(code);e.setErrorMessage(message);e.setProcessDate(now.toLocalDate());e.setProcessTime(now.toLocalTime());e.setUserId("SYSTEM");e.setAdditionalInfo(additionalInfo);repository.save(e);
  switch(severity){case INFO->log.info("{} {}: {}",program,code,message);case WARNING->log.warn("{} {}: {}",program,code,message);case ERROR,SEVERE->log.error("{} {}: {}",program,code,message);}
 }
 public String determineAction(ErrorSeverity severity){return severity==ErrorSeverity.SEVERE?"ABEND":(severity==ErrorSeverity.WARNING||severity==ErrorSeverity.INFO?"CONTINUE":"RETURN");}
}
