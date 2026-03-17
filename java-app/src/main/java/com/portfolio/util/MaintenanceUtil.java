package com.portfolio.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Maintenance Utility.
 * Replaces: UTLMNT00.cbl utility helper functions.
 * Provides date calculation and formatting utilities for maintenance operations.
 */
public final class MaintenanceUtil {

    private static final DateTimeFormatter COBOL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private MaintenanceUtil() {
        // Utility class - no instantiation
    }

    /**
     * Converts a COBOL date string (YYYYMMDD) to LocalDate.
     */
    public static LocalDate parseCobolDate(String cobolDate) {
        if (cobolDate == null || cobolDate.isBlank() || cobolDate.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(cobolDate, COBOL_DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Formats a LocalDate as a COBOL date string (YYYYMMDD).
     */
    public static String formatCobolDate(LocalDate date) {
        if (date == null) {
            return "00000000";
        }
        return date.format(COBOL_DATE_FORMAT);
    }

    /**
     * Calculates retention cutoff date.
     * Used for purging old records.
     */
    public static LocalDate calculateRetentionCutoff(int retentionDays) {
        return LocalDate.now().minusDays(retentionDays);
    }

    /**
     * Checks if a date string represents a valid COBOL date.
     */
    public static boolean isValidCobolDate(String dateStr) {
        return parseCobolDate(dateStr) != null;
    }
}
