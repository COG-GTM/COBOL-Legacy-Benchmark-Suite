package com.portfolio.web;

import com.portfolio.config.ErrorLoggingService;
import com.portfolio.domain.*;
import com.portfolio.service.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalExceptionHandler {
  private final ErrorLoggingService errorLoggingService;

  public GlobalExceptionHandler(ErrorLoggingService errorLoggingService) {
    this.errorLoggingService = errorLoggingService;
  }

  @ExceptionHandler(BusinessException.class)
  public Object business(BusinessException businessException) {
    errorLoggingService.log(
        "WEB",
        ErrorType.APPLICATION,
        ErrorSeverity.WARNING,
        businessException.getCode(),
        businessException.getMessage(),
        "");
    return response(businessException.getCode(), businessException.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public Object notFound(ResourceNotFoundException resourceNotFoundException) {
    return response("E002", resourceNotFoundException.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public Object denied(AccessDeniedException accessDeniedException) {
    return response("E006", accessDeniedException.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public Object generic(Exception exception) {
    errorLoggingService.log(
        "WEB", ErrorType.SYSTEM, ErrorSeverity.SEVERE, "E007", exception.getMessage(), "");
    return response("E007", "Internal server error");
  }

  private ResponseEntity<Map<String, String>> response(String code, String message) {
    return ResponseEntity.status(
            code.equals("E002")
                ? HttpStatus.NOT_FOUND
                : code.equals("E006")
                    ? HttpStatus.FORBIDDEN
                    : code.equals("E007")
                        ? HttpStatus.INTERNAL_SERVER_ERROR
                        : HttpStatus.BAD_REQUEST)
        .body(Map.of("code", code, "message", message));
  }
}
