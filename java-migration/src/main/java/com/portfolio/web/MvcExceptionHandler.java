package com.portfolio.web;

import com.portfolio.config.ErrorLoggingService;
import com.portfolio.domain.ErrorSeverity;
import com.portfolio.domain.ErrorType;
import com.portfolio.service.BusinessException;
import com.portfolio.service.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = {InquiryController.class})
public class MvcExceptionHandler {
  private final ErrorLoggingService errorLoggingService;

  public MvcExceptionHandler(ErrorLoggingService errorLoggingService) {
    this.errorLoggingService = errorLoggingService;
  }

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ModelAndView business(BusinessException exception) {
    logWarning(exception.getCode(), exception.getMessage());
    return error(exception.getCode(), exception.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView notFound(ResourceNotFoundException exception) {
    logWarning("E002", exception.getMessage());
    return error("E002", exception.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ModelAndView denied(AccessDeniedException exception) {
    logWarning("E006", exception.getMessage());
    return error("E006", exception.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ModelAndView generic(Exception exception) {
    errorLoggingService.log(
        "WEB", ErrorType.SYSTEM, ErrorSeverity.SEVERE, "E007", exception.getMessage(), "");
    return error("E007", "Internal server error");
  }

  private ModelAndView error(String code, String details) {
    ModelAndView modelAndView = new ModelAndView("error");
    modelAndView.addObject("errorCode", code);
    modelAndView.addObject("errorDetails", details);
    return modelAndView;
  }

  private void logWarning(String code, String message) {
    errorLoggingService.log("WEB", ErrorType.APPLICATION, ErrorSeverity.WARNING, code, message, "");
  }
}
