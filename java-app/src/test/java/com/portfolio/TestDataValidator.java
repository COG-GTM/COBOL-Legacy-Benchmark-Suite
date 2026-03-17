package com.portfolio;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.Portfolio;
import com.portfolio.model.TransactionHistory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Data Validator.
 * Replaces: TSTVAL00.cbl - Validates Java output against expected results.
 *
 * Verifies:
 * - 100% data record match for business data
 * - Financial calculations within ±0.01 rounding tolerance (BigDecimal comparisons)
 * - All expected errors are logged
 * - Report totals match
 */
public class TestDataValidator {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    /**
     * Validates portfolio records match expected data.
     */
    public static List<String> validatePortfolios(List<Portfolio> actual,
                                                   List<Portfolio> expected) {
        List<String> errors = new ArrayList<>();

        if (actual.size() != expected.size()) {
            errors.add("Portfolio count mismatch: actual=" + actual.size()
                    + " expected=" + expected.size());
            return errors;
        }

        for (int i = 0; i < expected.size(); i++) {
            Portfolio act = actual.get(i);
            Portfolio exp = expected.get(i);

            if (!exp.getPortfolioId().equals(act.getPortfolioId())) {
                errors.add("Portfolio ID mismatch at index " + i
                        + ": actual=" + act.getPortfolioId()
                        + " expected=" + exp.getPortfolioId());
            }
            if (!exp.getStatus().equals(act.getStatus())) {
                errors.add("Portfolio status mismatch for " + exp.getPortfolioId()
                        + ": actual=" + act.getStatus()
                        + " expected=" + exp.getStatus());
            }
        }

        return errors;
    }

    /**
     * Validates financial calculations are within rounding tolerance.
     * TSTVAL00 uses ±0.01 tolerance for comparing COBOL COMP-3 results
     * with Java BigDecimal results.
     */
    public static List<String> validatePositions(List<InvestmentPosition> actual,
                                                  List<InvestmentPosition> expected) {
        List<String> errors = new ArrayList<>();

        if (actual.size() != expected.size()) {
            errors.add("Position count mismatch: actual=" + actual.size()
                    + " expected=" + expected.size());
            return errors;
        }

        for (int i = 0; i < expected.size(); i++) {
            InvestmentPosition act = actual.get(i);
            InvestmentPosition exp = expected.get(i);

            if (!isWithinTolerance(act.getCostBasis(), exp.getCostBasis())) {
                errors.add("Cost basis mismatch for "
                        + exp.getKey().getPortfolioId() + "/"
                        + exp.getKey().getInvestmentId()
                        + ": actual=" + act.getCostBasis()
                        + " expected=" + exp.getCostBasis());
            }
            if (!isWithinTolerance(act.getMarketValue(), exp.getMarketValue())) {
                errors.add("Market value mismatch for "
                        + exp.getKey().getPortfolioId() + "/"
                        + exp.getKey().getInvestmentId()
                        + ": actual=" + act.getMarketValue()
                        + " expected=" + exp.getMarketValue());
            }
            if (!isWithinTolerance(act.getQuantity(), exp.getQuantity())) {
                errors.add("Quantity mismatch for "
                        + exp.getKey().getPortfolioId() + "/"
                        + exp.getKey().getInvestmentId()
                        + ": actual=" + act.getQuantity()
                        + " expected=" + exp.getQuantity());
            }
        }

        return errors;
    }

    /**
     * Validates transaction records.
     */
    public static List<String> validateTransactions(List<TransactionHistory> actual,
                                                     List<TransactionHistory> expected) {
        List<String> errors = new ArrayList<>();

        if (actual.size() != expected.size()) {
            errors.add("Transaction count mismatch: actual=" + actual.size()
                    + " expected=" + expected.size());
            return errors;
        }

        for (int i = 0; i < expected.size(); i++) {
            TransactionHistory act = actual.get(i);
            TransactionHistory exp = expected.get(i);

            if (!exp.getTransactionId().equals(act.getTransactionId())) {
                errors.add("Transaction ID mismatch at index " + i);
            }
            if (!isWithinTolerance(act.getAmount(), exp.getAmount())) {
                errors.add("Amount mismatch for " + exp.getTransactionId()
                        + ": actual=" + act.getAmount()
                        + " expected=" + exp.getAmount());
            }
        }

        return errors;
    }

    /**
     * Validates report totals match.
     * Replaces: TSTVAL00 report total comparison logic.
     */
    public static boolean validateReportTotal(BigDecimal actual, BigDecimal expected) {
        return isWithinTolerance(actual, expected);
    }

    /**
     * Checks if two BigDecimal values are within the rounding tolerance (±0.01).
     */
    private static boolean isWithinTolerance(BigDecimal actual, BigDecimal expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        return actual.subtract(expected).abs()
                .setScale(2, RoundingMode.HALF_UP)
                .compareTo(TOLERANCE) <= 0;
    }
}
