package com.portfolio.util;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.TransactionRecord;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PortfolioValidation {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    private static final String ID_PREFIX = "PORT";
    private static final Set<String> VALID_TYPES = Set.of("BU", "SL", "TR", "FE");
    private static final Set<String> VALID_STATUSES = Set.of("A", "C", "S");
    private static final Set<String> VALID_CLIENT_TYPES = Set.of("I", "C", "T");

    private PortfolioValidation() {}

    public static List<String> validatePortfolio(Portfolio portfolio) {
        List<String> errors = new ArrayList<>();
        if (portfolio.getPortfolioId() == null || portfolio.getPortfolioId().isBlank()) {
            errors.add("Invalid Portfolio ID format");
        }
        if (portfolio.getClientId() == null || portfolio.getClientId().isBlank()) {
            errors.add("Client ID is required");
        }
        if (portfolio.getPortfolioName() == null || portfolio.getPortfolioName().isBlank()) {
            errors.add("Portfolio name is required");
        }
        if (portfolio.getStatus() != null && !VALID_STATUSES.contains(portfolio.getStatus())) {
            errors.add("Invalid status code");
        }
        if (portfolio.getClientType() != null && !VALID_CLIENT_TYPES.contains(portfolio.getClientType())) {
            errors.add("Invalid client type");
        }
        if (portfolio.getTotalValue() != null) {
            if (portfolio.getTotalValue().compareTo(MIN_AMOUNT) < 0
                    || portfolio.getTotalValue().compareTo(MAX_AMOUNT) > 0) {
                errors.add("Amount outside valid range");
            }
        }
        return errors;
    }

    public static List<String> validateTransaction(TransactionRecord transaction) {
        List<String> errors = new ArrayList<>();
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().isBlank()) {
            errors.add("Portfolio ID is required");
        }
        if (transaction.getTransactionType() == null
                || !VALID_TYPES.contains(transaction.getTransactionType())) {
            errors.add("Invalid Transaction Type: " + transaction.getTransactionType());
        }
        if (transaction.getQuantity() == null) {
            errors.add("Quantity is required");
        } else if (transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Quantity must be greater than zero");
        }
        if (!"TR".equals(transaction.getTransactionType())) {
            if (transaction.getPrice() == null) {
                errors.add("Price is required");
            } else if (transaction.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Price must be greater than zero");
            }
            if (transaction.getAmount() != null
                    && transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Amount must be greater than zero");
            }
        }
        return errors;
    }
}
