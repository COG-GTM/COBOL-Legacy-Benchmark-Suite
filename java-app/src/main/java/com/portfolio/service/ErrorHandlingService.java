package com.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Error Handling Service.
 * Replaces: ERRPROC.cbl and DB2ERR.cbl - Centralized error logging,
 * error code mapping, and error description formatting.
 *
 * Error categories from ERRHAND.cpy:
 * - VS = VSAM (now JPA/DB) errors
 * - VL = Validation errors
 * - PR = Processing errors
 * - SY = System errors
 *
 * Return codes from ERRHAND.cpy:
 * - 0 = Success
 * - 4 = Warning
 * - 8 = Error
 * - 12 = Severe
 * - 16 = Terminal
 */
@Service
public class ErrorHandlingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingService.class);

    public static final int RC_SUCCESS = 0;
    public static final int RC_WARNING = 4;
    public static final int RC_ERROR = 8;
    public static final int RC_SEVERE = 12;
    public static final int RC_TERMINAL = 16;

    private static final Map<String, String> ERROR_CATEGORIES = Map.of(
            "VS", "Database Error",
            "VL", "Validation Error",
            "PR", "Processing Error",
            "SY", "System Error"
    );

    /**
     * Logs an error with full context.
     * Replaces ERRPROC.cbl error logging routine.
     */
    public void logError(String program, String category, String code,
                         int severity, String text, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String categoryDesc = ERROR_CATEGORIES.getOrDefault(category, "Unknown");

        String formattedMessage = String.format(
                "[%s] %s (%s) - %s-%s: Severity=%d, %s",
                timestamp, program, categoryDesc, category, code, severity, text
        );

        if (severity >= RC_TERMINAL) {
            log.error("{} | Details: {}", formattedMessage, details);
        } else if (severity >= RC_ERROR) {
            log.error("{}", formattedMessage);
        } else if (severity >= RC_WARNING) {
            log.warn("{}", formattedMessage);
        } else {
            log.info("{}", formattedMessage);
        }
    }

    /**
     * Formats an error description.
     * Replaces the error message formatting in ERRPROC.cbl.
     */
    public String formatErrorDescription(String category, String code, String text) {
        String categoryDesc = ERROR_CATEGORIES.getOrDefault(category, "Unknown");
        return String.format("%s [%s-%s]: %s", categoryDesc, category, code, text);
    }

    /**
     * Maps a database exception to an error code.
     * Replaces DB2ERR.cbl SQLCODE handling.
     */
    public String mapDatabaseError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return "VS-9999: Unknown database error";
        }
        if (message.contains("duplicate") || message.contains("unique")) {
            return "VS-0022: Duplicate record key";
        }
        if (message.contains("not found") || message.contains("no result")) {
            return "VS-0023: Record not found";
        }
        return "VS-9999: Database error - " + message;
    }
}
