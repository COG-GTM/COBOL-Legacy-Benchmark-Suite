package com.clbs.portfolio.exception;

import lombok.Getter;

/**
 * Exception for legacy VSAM-style file operations.
 * Provides compatibility logging for migrated data access patterns.
 * Status codes from COBOL ERRHAND.cpy (ERR-VSAM-STATUSES).
 */
@Getter
public class VsamFileException extends RuntimeException {

    private final String fileStatus;
    private final String fileName;

    public VsamFileException(String message, String fileStatus, String fileName) {
        super(message);
        this.fileStatus = fileStatus;
        this.fileName = fileName;
    }

    public VsamFileException(String message, Throwable cause, String fileStatus, String fileName) {
        super(message, cause);
        this.fileStatus = fileStatus;
        this.fileName = fileName;
    }
}
