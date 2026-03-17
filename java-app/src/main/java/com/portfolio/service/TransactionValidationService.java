package com.portfolio.service;

import com.portfolio.exception.TransactionValidationException;
import com.portfolio.model.Portfolio;
import com.portfolio.model.TransactionHistory;
import com.portfolio.model.enums.TransactionType;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Transaction Validation Service.
 * Replaces: TRNVAL00.cbl - Validates incoming financial transactions.
 *
 * Each validation paragraph from the COBOL PROCEDURE DIVISION
 * becomes a private method in this class.
 */
@Service
public class TransactionValidationService {

    private static final Logger log = LoggerFactory.getLogger(TransactionValidationService.class);

    private static final Set<String> VALID_TRANSACTION_TYPES = Set.of("BU", "SL", "TR", "FE");
    private static final Set<String> VALID_CURRENCIES = Set.of("USD", "EUR", "GBP", "JPY", "CAD", "CHF");

    private final PortfolioRepository portfolioRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public TransactionValidationService(PortfolioRepository portfolioRepository,
                                        TransactionHistoryRepository transactionHistoryRepository) {
        this.portfolioRepository = portfolioRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    /**
     * Validates a transaction record.
     * Replaces the main validation loop in TRNVAL00.cbl PROCEDURE DIVISION.
     */
    public List<String> validate(TransactionHistory transaction) {
        List<String> errors = new ArrayList<>();

        validatePortfolioExists(transaction, errors);
        validateInvestmentId(transaction, errors);
        validateTransactionType(transaction, errors);
        validateAmounts(transaction, errors);
        validateCurrency(transaction, errors);
        checkDuplicateTransaction(transaction, errors);

        if (!errors.isEmpty()) {
            log.warn("Transaction {} failed validation with {} errors",
                    transaction.getTransactionId(), errors.size());
        }

        return errors;
    }

    /**
     * Validates that the portfolio exists and is active.
     * Replaces TRNVAL00 portfolio existence check paragraph.
     */
    private void validatePortfolioExists(TransactionHistory transaction, List<String> errors) {
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().isBlank()) {
            errors.add("Portfolio ID is required");
            return;
        }

        Optional<Portfolio> portfolio = portfolioRepository.findById(transaction.getPortfolioId().trim());
        if (portfolio.isEmpty()) {
            errors.add("Portfolio not found: " + transaction.getPortfolioId());
        } else if (!"A".equals(portfolio.get().getStatus())) {
            errors.add("Portfolio is not active: " + transaction.getPortfolioId());
        }
    }

    /**
     * Validates the investment ID format.
     */
    private void validateInvestmentId(TransactionHistory transaction, List<String> errors) {
        if (transaction.getInvestmentId() == null || transaction.getInvestmentId().isBlank()) {
            errors.add("Investment ID is required");
        }
    }

    /**
     * Validates transaction type code (BU/SL/TR/FE).
     * Replaces the level-88 condition checks from TRNREC.cpy.
     */
    private void validateTransactionType(TransactionHistory transaction, List<String> errors) {
        if (transaction.getTransactionType() == null
                || !VALID_TRANSACTION_TYPES.contains(transaction.getTransactionType())) {
            errors.add("Invalid transaction type: " + transaction.getTransactionType()
                    + ". Valid types: BU=Buy, SL=Sell, TR=Transfer, FE=Fee");
        }
    }

    /**
     * Validates financial amounts using BigDecimal arithmetic.
     * All arithmetic uses BigDecimal to match COBOL fixed-point behavior.
     */
    private void validateAmounts(TransactionHistory transaction, List<String> errors) {
        if (transaction.getQuantity() == null || transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Quantity must be positive");
        }

        if (transaction.getPrice() == null || transaction.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Price must be positive");
        }

        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount must be positive");
        }

        // Verify amount = quantity * price (within rounding tolerance)
        if (transaction.getQuantity() != null && transaction.getPrice() != null
                && transaction.getAmount() != null) {
            BigDecimal expectedAmount = transaction.getQuantity()
                    .multiply(transaction.getPrice())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal difference = transaction.getAmount().subtract(expectedAmount).abs();
            if (difference.compareTo(new BigDecimal("0.01")) > 0) {
                errors.add("Amount does not match quantity * price. Expected: "
                        + expectedAmount + ", Got: " + transaction.getAmount());
            }
        }
    }

    /**
     * Validates currency code.
     */
    private void validateCurrency(TransactionHistory transaction, List<String> errors) {
        if (transaction.getCurrencyCode() == null
                || !VALID_CURRENCIES.contains(transaction.getCurrencyCode().trim())) {
            errors.add("Invalid currency code: " + transaction.getCurrencyCode());
        }
    }

    /**
     * Checks for duplicate transactions.
     */
    private void checkDuplicateTransaction(TransactionHistory transaction, List<String> errors) {
        if (transaction.getTransactionId() != null) {
            Optional<TransactionHistory> existing =
                    transactionHistoryRepository.findById(transaction.getTransactionId());
            if (existing.isPresent()) {
                errors.add("Duplicate transaction ID: " + transaction.getTransactionId());
            }
        }
    }

    /**
     * Validates and throws if invalid.
     */
    public void validateAndThrow(TransactionHistory transaction) {
        List<String> errors = validate(transaction);
        if (!errors.isEmpty()) {
            throw new TransactionValidationException(
                    "Transaction validation failed: " + String.join("; ", errors));
        }
    }
}
