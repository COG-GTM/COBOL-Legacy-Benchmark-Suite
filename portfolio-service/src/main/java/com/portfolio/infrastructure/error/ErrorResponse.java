package com.portfolio.infrastructure.error;

import com.portfolio.domain.model.ErrorCategory;
import com.portfolio.domain.model.ErrorSeverity;
import java.time.LocalDateTime;

/**
 * Standard error response DTO. Maps COBOL ERRHAND.cpy ERR-MESSAGE structure.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        String errorCode,
        ErrorCategory category,
        ErrorSeverity severity,
        String message,
        String details
) {
    public static ErrorResponse of(String errorCode, ErrorCategory category,
                                   ErrorSeverity severity, String message, String details) {
        return new ErrorResponse(LocalDateTime.now(), errorCode, category, severity, message, details);
    }
}
