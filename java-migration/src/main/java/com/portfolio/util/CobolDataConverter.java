package com.portfolio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for converting COBOL data types to Java types
 * Handles COMP-3 packed decimal, dates, and other COBOL-specific formats
 */
public final class CobolDataConverter {

    private static final DateTimeFormatter COBOL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter COBOL_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    private CobolDataConverter() {
        // Utility class - prevent instantiation
    }

    /**
     * Parse COBOL COMP-3 packed decimal to BigDecimal
     * COBOL: PIC S9(precision)V9(scale) COMP-3
     * 
     * In a real migration, this would handle actual packed decimal bytes.
     * For flat file exports, the data is typically converted to display format.
     * 
     * @param value The string representation of the packed decimal
     * @param precision Total number of digits
     * @param scale Number of decimal places
     * @return BigDecimal representation
     */
    public static BigDecimal parsePackedDecimal(String value, int precision, int scale) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String trimmed = value.trim();
        
        // Handle sign
        boolean negative = false;
        if (trimmed.endsWith("-") || trimmed.endsWith("}") || trimmed.endsWith("J") ||
            trimmed.endsWith("K") || trimmed.endsWith("L") || trimmed.endsWith("M") ||
            trimmed.endsWith("N") || trimmed.endsWith("O") || trimmed.endsWith("P") ||
            trimmed.endsWith("Q") || trimmed.endsWith("R")) {
            negative = true;
            trimmed = convertSignedOverpunch(trimmed);
        } else if (trimmed.startsWith("-")) {
            negative = true;
            trimmed = trimmed.substring(1);
        } else if (trimmed.startsWith("+")) {
            trimmed = trimmed.substring(1);
        }

        // Remove any non-numeric characters except decimal point
        trimmed = trimmed.replaceAll("[^0-9.]", "");

        if (trimmed.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal result;
            if (trimmed.contains(".")) {
                result = new BigDecimal(trimmed);
            } else {
                // Apply implied decimal point
                result = new BigDecimal(trimmed).movePointLeft(scale);
            }
            
            if (negative) {
                result = result.negate();
            }
            
            return result.setScale(scale, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Convert COBOL signed overpunch character to digit
     * COBOL uses overpunch characters to represent sign in the last digit
     */
    private static String convertSignedOverpunch(String value) {
        if (value == null || value.isEmpty()) {
            return "0";
        }
        
        char lastChar = value.charAt(value.length() - 1);
        String prefix = value.substring(0, value.length() - 1);
        
        // Positive overpunch: { A B C D E F G H I = 0 1 2 3 4 5 6 7 8 9
        // Negative overpunch: } J K L M N O P Q R = 0 1 2 3 4 5 6 7 8 9
        String digit;
        switch (lastChar) {
            case '{': case '}': digit = "0"; break;
            case 'A': case 'J': digit = "1"; break;
            case 'B': case 'K': digit = "2"; break;
            case 'C': case 'L': digit = "3"; break;
            case 'D': case 'M': digit = "4"; break;
            case 'E': case 'N': digit = "5"; break;
            case 'F': case 'O': digit = "6"; break;
            case 'G': case 'P': digit = "7"; break;
            case 'H': case 'Q': digit = "8"; break;
            case 'I': case 'R': digit = "9"; break;
            default: digit = String.valueOf(lastChar);
        }
        
        return prefix + digit;
    }

    /**
     * Parse COBOL date (YYYYMMDD) to LocalDate
     */
    public static LocalDate parseCobolDate(String cobolDate) {
        if (cobolDate == null || cobolDate.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = cobolDate.trim();
        if (trimmed.equals("00000000") || trimmed.length() != 8) {
            return null;
        }
        
        try {
            return LocalDate.parse(trimmed, COBOL_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parse COBOL time (HHMMSS) to LocalTime
     */
    public static LocalTime parseCobolTime(String cobolTime) {
        if (cobolTime == null || cobolTime.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = cobolTime.trim();
        if (trimmed.equals("000000") || trimmed.length() != 6) {
            return null;
        }
        
        try {
            return LocalTime.parse(trimmed, COBOL_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Format LocalDate to COBOL date format (YYYYMMDD)
     */
    public static String formatToCobolDate(LocalDate date) {
        if (date == null) {
            return "00000000";
        }
        return date.format(COBOL_DATE_FORMAT);
    }

    /**
     * Format LocalTime to COBOL time format (HHMMSS)
     */
    public static String formatToCobolTime(LocalTime time) {
        if (time == null) {
            return "000000";
        }
        return time.format(COBOL_TIME_FORMAT);
    }

    /**
     * Parse COBOL alphanumeric field (trim and handle spaces)
     */
    public static String parseAlphanumeric(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    /**
     * Parse COBOL numeric field to integer
     */
    public static int parseNumeric(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parse COBOL numeric field to long
     */
    public static long parseNumericLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
