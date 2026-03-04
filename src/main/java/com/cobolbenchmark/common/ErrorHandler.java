package com.cobolbenchmark.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Error Handler - migrated from ERRHAND.cpy.
 * Centralized error message formatting and VSAM status code handling.
 */
public class ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    // Error message constants from ERRHAND.cpy
    public static final String ERR_RECORD_NOT_FOUND = "Record not found";
    public static final String ERR_DUPLICATE_KEY = "Duplicate key on write";
    public static final String ERR_FILE_NOT_OPEN = "File not open";
    public static final String ERR_IO_ERROR = "I/O error occurred";
    public static final String ERR_INVALID_KEY = "Invalid key specification";
    public static final String ERR_NO_SPACE = "No space available on file";
    public static final String ERR_RECORD_LOCKED = "Record locked by another task";
    public static final String ERR_END_OF_FILE = "End of file reached";

    private ErrorHandler() {
        // Utility class
    }

    /**
     * Format error message from VSAM status code - replaces ERRHAND.cpy error routine.
     */
    public static String formatVsamError(String fileStatus, String fileName) {
        try {
            VsamStatus status = VsamStatus.fromCode(fileStatus);
            return String.format("VSAM %s on file %s: %s", status.getCode(), fileName, status.getDescription());
        } catch (IllegalArgumentException e) {
            return String.format("VSAM unknown status %s on file %s", fileStatus, fileName);
        }
    }

    /**
     * Log an error with program context - replaces PERFORM 9000-ERROR-ROUTINE.
     */
    public static void logError(String programId, String paragraphName, String message) {
        logger.error("Program: {} Paragraph: {} - {}", programId, paragraphName, message);
    }

    /**
     * Log a warning with program context.
     */
    public static void logWarning(String programId, String paragraphName, String message) {
        logger.warn("Program: {} Paragraph: {} - {}", programId, paragraphName, message);
    }

    /**
     * Handle VSAM file status and throw appropriate exception if error.
     */
    public static void handleVsamStatus(String fileStatus, String fileName, String operation) {
        if ("00".equals(fileStatus) || "10".equals(fileStatus)) {
            return; // Success or EOF
        }
        VsamStatus status;
        try {
            status = VsamStatus.fromCode(fileStatus);
        } catch (IllegalArgumentException e) {
            throw new ApplicationException("VSAMERR", "Unknown VSAM status " + fileStatus + " on " + fileName);
        }

        switch (status) {
            case NOTFND:
                throw new RecordNotFoundException(fileName, operation);
            case DUPKEY:
                throw new DuplicateRecordException(fileName, operation);
            default:
                throw new ApplicationException("VSAMERR", formatVsamError(fileStatus, fileName));
        }
    }
}
