package com.clbs.portfolio.web;

import com.clbs.portfolio.exception.ErrorCode;
import com.clbs.portfolio.exception.PortfolioException;
import com.clbs.portfolio.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Maps exceptions to {@link ErrorResponse}, replacing ERRPROC / ERRHNDL message formatting. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorResponseWriter errors;

    public GlobalExceptionHandler(ErrorResponseWriter errors) {
        this.errors = errors;
    }

    @ExceptionHandler(PortfolioException.class)
    ResponseEntity<ErrorResponse> handlePortfolio(PortfolioException e, HttpServletRequest req) {
        HttpStatus status = switch (e.getErrorCode()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE -> HttpStatus.CONFLICT;
            case VALIDATION, INVALID_DATA -> HttpStatus.BAD_REQUEST;
            case PROCESSING -> HttpStatus.UNPROCESSABLE_ENTITY;
            case SECURITY -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return respond(status, e.getErrorCode(), e.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<ErrorResponse.FieldErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldErrorDetail(f.getField(), f.getDefaultMessage()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, "Request validation failed", req, details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException e,
                                                            HttpServletRequest req) {
        List<ErrorResponse.FieldErrorDetail> details = e.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream().map(err -> new ErrorResponse.FieldErrorDetail(
                        r.getMethodParameter().getParameterName(), err.getDefaultMessage())))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, "Request validation failed", req, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException e, HttpServletRequest req) {
        List<ErrorResponse.FieldErrorDetail> details = e.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, "Request validation failed", req, details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> handleBadInput(Exception e, HttpServletRequest req) {
        String message = e instanceof HttpMessageNotReadableException ? "Malformed request body" : e.getMessage();
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATA, message, req, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.INVALID_DATA, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.INVALID_DATA, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Resource not found", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.PROCESSING, "Unexpected error", req, List.of());
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, ErrorCode code, String message,
                                                  HttpServletRequest req,
                                                  List<ErrorResponse.FieldErrorDetail> details) {
        return ResponseEntity.status(status).body(errors.body(status, code, message, req.getRequestURI(), details));
    }
}
