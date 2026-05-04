package com.portfolio.controller;

import com.portfolio.service.DatabaseErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final DatabaseErrorHandler errorHandler;

    public GlobalExceptionHandler(DatabaseErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleValidationError(IllegalArgumentException ex, Model model) {
        log.warn("Validation error: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorType", "Validation Error");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralError(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        errorHandler.logSystemError("ERRHNDL", ex.getMessage(), ex);
        model.addAttribute("errorMessage", "An unexpected error occurred: " + ex.getMessage());
        model.addAttribute("errorType", "System Error");
        return "error";
    }
}
