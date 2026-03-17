package com.portfolio.util;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Validation Utility.
 * Replaces: UTLVAL00.cbl utility helper functions.
 * Provides common validation routines used across services.
 */
public final class ValidationUtil {

    /** Valid portfolio statuses from COBOL level-88 definitions */
    private static final Set<String> VALID_PORTFOLIO_STATUSES = Set.of("A", "C", "S");

    /** Valid transaction types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee */
    private static final Set<String> VALID_TRANSACTION_TYPES = Set.of("BU", "SL", "TR", "FE");

    /** Valid transaction statuses: P=Processed, F=Failed, R=Reversed */
    private static final Set<String> VALID_TRANSACTION_STATUSES = Set.of("P", "F", "R");

    /** Valid batch statuses: R=Ready, A=Active, W=Waiting, D=Done, E=Error */
    private static final Set<String> VALID_BATCH_STATUSES = Set.of("R", "A", "W", "D", "E");

    private ValidationUtil() {
        // Utility class - no instantiation
    }

    /**
     * Validates a portfolio ID format.
     * COBOL: PIC X(8) - 8-character field.
     */
    public static boolean isValidPortfolioId(String portfolioId) {
        return portfolioId != null
                && !portfolioId.isBlank()
                && portfolioId.trim().length() <= 8;
    }

    /**
     * Validates an investment ID format.
     * COBOL: PIC X(10) - 10-character field.
     */
    public static boolean isValidInvestmentId(String investmentId) {
        return investmentId != null
                && !investmentId.isBlank()
                && investmentId.trim().length() <= 10;
    }

    /**
     * Validates a portfolio status code.
     */
    public static boolean isValidPortfolioStatus(String status) {
        return status != null && VALID_PORTFOLIO_STATUSES.contains(status);
    }

    /**
     * Validates a transaction type code.
     */
    public static boolean isValidTransactionType(String type) {
        return type != null && VALID_TRANSACTION_TYPES.contains(type);
    }

    /**
     * Validates a transaction status code.
     */
    public static boolean isValidTransactionStatus(String status) {
        return status != null && VALID_TRANSACTION_STATUSES.contains(status);
    }

    /**
     * Validates a batch status code.
     */
    public static boolean isValidBatchStatus(String status) {
        return status != null && VALID_BATCH_STATUSES.contains(status);
    }

    /**
     * Validates that a financial amount is positive.
     * Replaces: COBOL numeric validation checks.
     */
    public static boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Validates that a financial amount is non-negative.
     */
    public static boolean isNonNegativeAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Validates a currency code (3-character ISO).
     * COBOL: PIC X(3).
     */
    public static boolean isValidCurrencyCode(String currencyCode) {
        return currencyCode != null
                && currencyCode.trim().length() == 3
                && currencyCode.trim().chars().allMatch(Character::isLetter);
    }

    /**
     * Pads a string to the right with spaces to match COBOL PIC X(n) behavior.
     */
    public static String padRight(String value, int length) {
        if (value == null) {
            return " ".repeat(length);
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }

    /**
     * Trims a COBOL-style padded string.
     */
    public static String trimCobolString(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
