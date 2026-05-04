package com.portfolio.web;

import com.portfolio.exception.ApplicationException;
import com.portfolio.exception.PortfolioSecurityException;
import com.portfolio.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Online Error Handler - migrated from COBOL ERRHNDL.cbl.
 * Spring @ControllerAdvice replaces CICS error screen display
 * with REST error responses or Thymeleaf error pages.
 */
@ControllerAdvice
public class OnlineErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(OnlineErrorHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ModelAndView handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return createErrorView(HttpStatus.BAD_REQUEST, "Validation Error", ex.getMessage(),
                ex.getErrorCode());
    }

    @ExceptionHandler(PortfolioSecurityException.class)
    public ModelAndView handleSecurityException(PortfolioSecurityException ex) {
        log.error("Security error: {}", ex.getMessage());
        return createErrorView(HttpStatus.FORBIDDEN, "Security Error", ex.getMessage(),
                ex.getErrorCode());
    }

    @ExceptionHandler(ApplicationException.class)
    public ModelAndView handleApplicationException(ApplicationException ex) {
        log.error("Application error: {} [RC={}]", ex.getMessage(), ex.getReturnCode());
        return createErrorView(HttpStatus.INTERNAL_SERVER_ERROR, "Application Error",
                ex.getMessage(), ex.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return createErrorView(HttpStatus.INTERNAL_SERVER_ERROR, "System Error",
                "An unexpected error occurred. Please contact support.",
                "E999");
    }

    private ModelAndView createErrorView(HttpStatus status, String title,
                                         String message, String errorCode) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("errorTitle", title);
        mav.addObject("errorMessage", message);
        mav.addObject("errorCode", errorCode);
        return mav;
    }
}
