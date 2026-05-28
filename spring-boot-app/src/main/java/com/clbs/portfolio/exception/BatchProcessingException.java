package com.clbs.portfolio.exception;

import com.clbs.portfolio.common.ReturnCode;
import lombok.Getter;

/**
 * Exception for batch processing failures.
 * Severity levels map to COBOL ERRHAND.cpy (ERR-RETURN-CODES).
 */
@Getter
public class BatchProcessingException extends RuntimeException {

    private final ReturnCode severity;
    private final String programId;

    public BatchProcessingException(String message, ReturnCode severity, String programId) {
        super(message);
        this.severity = severity;
        this.programId = programId;
    }

    public BatchProcessingException(String message, Throwable cause, ReturnCode severity, String programId) {
        super(message, cause);
        this.severity = severity;
        this.programId = programId;
    }
}
