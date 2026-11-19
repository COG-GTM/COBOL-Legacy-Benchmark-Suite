package com.clbs.portfolio.web;

import com.clbs.portfolio.exception.ErrorCode;
import com.clbs.portfolio.web.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

/** Builds {@link ErrorResponse} bodies, also for the security filter chain (outside MVC). */
@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ErrorResponse body(HttpStatus status, ErrorCode code, String message, String path,
                              List<ErrorResponse.FieldErrorDetail> details) {
        return new ErrorResponse(OffsetDateTime.now(), status.value(), status.getReasonPhrase(), code.code(), message,
                path, details);
    }

    public void write(HttpServletRequest req, HttpServletResponse res, HttpStatus status, ErrorCode code,
                      String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(), body(status, code, message, req.getRequestURI(), List.of()));
    }
}
