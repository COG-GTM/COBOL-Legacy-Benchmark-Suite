package com.portfolio.web;

import com.portfolio.config.ErrorLoggingService;
import com.portfolio.domain.ErrorSeverity;
import com.portfolio.domain.ErrorType;
import com.portfolio.service.BusinessException;
import com.portfolio.service.ResourceNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.portfolio.web.rest")
public class RestExceptionHandler {
  private final ErrorLoggingService errorLoggingService;

  public RestExceptionHandler(ErrorLoggingService errorLoggingService) {
    this.errorLoggingService = errorLoggingService;
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Map<String, String>> business(BusinessException exception) {
    logWarning(exception.getCode(), exception.getMessage());
    return response(exception.getCode(), exception.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> notFound(ResourceNotFoundException exception) {
    logWarning("E002", exception.getMessage());
    return response("E002", exception.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> denied(AccessDeniedException exception) {
    logWarning("E006", exception.getMessage());
    return response("E006", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException exception) {
    logWarning("E008", "Invalid request");
    return response("E008", "Invalid request");
  }

  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<Map<String, String>> unsupported(UnsupportedOperationException exception) {
    logWarning("E009", exception.getMessage());
    return response("E009", exception.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> generic(Exception exception) {
    errorLoggingService.log(
        "WEB", ErrorType.SYSTEM, ErrorSeverity.SEVERE, "E007", exception.getMessage(), "");
    return response("E007", "Internal server error");
  }

  private ResponseEntity<Map<String, String>> response(String code, String message) {
    return ResponseEntity.status(statusFor(code)).body(Map.of("code", code, "message", message));
  }

  private HttpStatus statusFor(String code) {
    return switch (code) {
      case "E002" -> HttpStatus.NOT_FOUND;
      case "E006" -> HttpStatus.FORBIDDEN;
      case "E007" -> HttpStatus.INTERNAL_SERVER_ERROR;
      case "E009" -> HttpStatus.NOT_IMPLEMENTED;
      default -> HttpStatus.BAD_REQUEST;
    };
  }

  private void logWarning(String code, String message) {
    errorLoggingService.log("WEB", ErrorType.APPLICATION, ErrorSeverity.WARNING, code, message, "");
  }
}
