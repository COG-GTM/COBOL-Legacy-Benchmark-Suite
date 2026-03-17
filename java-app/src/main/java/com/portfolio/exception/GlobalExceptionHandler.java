package com.portfolio.exception;

import com.portfolio.service.ErrorHandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global Exception Handler.
 * Replaces: ERRHNDL.cbl and the ERRMAP BMS map.
 * Uses @ControllerAdvice to catch exceptions and render error.html
 * (replacing the ERRMAP red error message display).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorHandlingService errorHandlingService;

    public GlobalExceptionHandler(ErrorHandlingService errorHandlingService) {
        this.errorHandlingService = errorHandlingService;
    }

    @ExceptionHandler(PortfolioNotFoundException.class)
    public String handlePortfolioNotFound(PortfolioNotFoundException ex, Model model) {
        log.warn("Portfolio not found: {}", ex.getPortfolioId());
        errorHandlingService.logError("CONTROLLER", "VL", "0023",
                ErrorHandlingService.RC_WARNING, ex.getMessage(), null);

        model.addAttribute("errorCode", "VL-0023");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(TransactionValidationException.class)
    public String handleTransactionValidation(TransactionValidationException ex, Model model) {
        log.warn("Transaction validation failed: {}", ex.getMessage());
        errorHandlingService.logError("CONTROLLER", "VL", "0001",
                ErrorHandlingService.RC_ERROR, ex.getMessage(), null);

        model.addAttribute("errorCode", "VL-0001");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        errorHandlingService.logError("CONTROLLER", "SY", "9999",
                ErrorHandlingService.RC_SEVERE, ex.getMessage(),
                ex.getClass().getName());

        model.addAttribute("errorCode", "SY-9999");
        model.addAttribute("errorMessage", "An unexpected error occurred: " + ex.getMessage());
        return "error";
    }
}
