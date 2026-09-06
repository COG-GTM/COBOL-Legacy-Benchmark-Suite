package com.cog.portfolio.web;

import com.cog.portfolio.common.ErrorCode;
import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.common.ReturnCode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * ERRHNDL.cbl / ERRHND.cpy: uniform error response. HTML requests get ERRMAP
 * (error.html); API requests get a JSON body carrying the legacy error code.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(LocalDateTime timestamp, int returnCode, String errorCode, String message) {
    }

    @ExceptionHandler(PortfolioException.class)
    public Object handlePortfolio(PortfolioException e, HttpServletRequest request) {
        HttpStatus status = switch (e.getErrorCode()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE -> HttpStatus.CONFLICT;
            case INVALID_DATA, VALIDATION -> HttpStatus.BAD_REQUEST;
            case SECURITY -> HttpStatus.FORBIDDEN;
            case PROCESSING -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        log.warn("{} {} -> {} {}", request.getMethod(), request.getRequestURI(), e.getErrorCode(), e.getMessage());
        return respond(request, status, e.getReturnCode().code(), e.getErrorCode().code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        return respond(request, HttpStatus.BAD_REQUEST, ReturnCode.WARNING.code(), ErrorCode.VALIDATION.code(), message);
    }

    @ExceptionHandler(Exception.class)
    public Object handleOther(Exception e, HttpServletRequest request) throws Exception {
        if (e instanceof AccessDeniedException) {
            throw e;
        }
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return respond(request, HttpStatus.INTERNAL_SERVER_ERROR, ReturnCode.SEVERE.code(), ErrorCode.PROCESSING.code(),
                "System error - contact support");
    }

    private Object respond(HttpServletRequest request, HttpStatus status, int rc, String code, String message) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), rc, code, message);
        if (wantsHtml(request)) {
            ModelAndView mav = new ModelAndView("error");
            mav.setStatus(status);
            mav.addObject("error", body);
            return mav;
        }
        return ResponseEntity.status(status).body(body);
    }

    private static boolean wantsHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        boolean api = uri.startsWith("/api/") || uri.startsWith("/batch/") || uri.startsWith("/maintenance/")
                || uri.startsWith("/validation/");
        return !api && accept != null && accept.contains("text/html");
    }
}
