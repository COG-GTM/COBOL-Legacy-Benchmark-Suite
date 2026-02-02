package com.portfolio.service;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Position;
import com.portfolio.domain.Transaction;
import com.portfolio.exception.PortfolioException;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionRepository;
import com.portfolio.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction Validation Service - migrated from COBOL TRNVAL00
 * Validates incoming financial transactions before processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionValidationService {

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    /**
     * Validate a single transaction
     * Equivalent to COBOL 2000-VALIDATE-TRANSACTION
     */
    @Transactional
    public ValidationResult validateTransaction(Transaction transaction) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateRequiredFields(transaction, errors);
        
        if (errors.isEmpty()) {
            validatePortfolioExists(transaction, errors);
        }
        
        if (errors.isEmpty()) {
            validateTransactionType(transaction, errors);
            validateAmounts(transaction, errors, warnings);
            validateBusinessRules(transaction, errors, warnings);
        }

        ValidationResult result = new ValidationResult();
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        result.setWarnings(warnings);
        result.setReturnCode(calculateReturnCode(errors, warnings));

        if (result.isValid()) {
            transaction.setStatus(Transaction.TransactionStatus.P);
            transactionRepository.save(transaction);
            log.info("Transaction validated successfully: portfolio={}, type={}", 
                    transaction.getPortfolioId(), transaction.getTransactionType());
        } else {
            transaction.setStatus(Transaction.TransactionStatus.F);
            transactionRepository.save(transaction);
            log.warn("Transaction validation failed: portfolio={}, errors={}", 
                    transaction.getPortfolioId(), errors);
        }

        return result;
    }

    /**
     * Batch validate multiple transactions
     * Equivalent to COBOL batch processing loop
     */
    @Transactional
    public BatchValidationResult validateBatch(List<Transaction> transactions) {
        BatchValidationResult batchResult = new BatchValidationResult();
        int successCount = 0;
        int errorCount = 0;
        int warningCount = 0;

        for (Transaction transaction : transactions) {
            ValidationResult result = validateTransaction(transaction);
            if (result.isValid()) {
                successCount++;
                if (!result.getWarnings().isEmpty()) {
                    warningCount++;
                }
            } else {
                errorCount++;
            }
            batchResult.getResults().add(result);
        }

        batchResult.setTotalProcessed(transactions.size());
        batchResult.setSuccessCount(successCount);
        batchResult.setErrorCount(errorCount);
        batchResult.setWarningCount(warningCount);
        batchResult.setReturnCode(calculateBatchReturnCode(errorCount, warningCount));

        log.info("Batch validation complete: total={}, success={}, errors={}, warnings={}",
                transactions.size(), successCount, errorCount, warningCount);

        return batchResult;
    }

    private void validateRequiredFields(Transaction transaction, List<String> errors) {
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().isBlank()) {
            errors.add("Portfolio ID is required");
        }
        if (transaction.getTransactionType() == null) {
            errors.add("Transaction type is required");
        }
        if (transaction.getTransactionDate() == null) {
            errors.add("Transaction date is required");
        }
        if (transaction.getAmount() == null) {
            errors.add("Transaction amount is required");
        }
    }

    private void validatePortfolioExists(Transaction transaction, List<String> errors) {
        Portfolio portfolio = portfolioRepository.findById(transaction.getPortfolioId()).orElse(null);
        if (portfolio == null) {
            errors.add("Portfolio not found: " + transaction.getPortfolioId());
            return;
        }
        if (portfolio.getStatus() != Portfolio.PortfolioStatus.A) {
            errors.add("Portfolio is not active: " + transaction.getPortfolioId());
        }
    }

    private void validateTransactionType(Transaction transaction, List<String> errors) {
        Transaction.TransactionType type = transaction.getTransactionType();
        
        if (type == Transaction.TransactionType.SL) {
            List<Position> positions = positionRepository.findActivePositionsByPortfolio(
                    transaction.getPortfolioId());
            
            boolean hasPosition = positions.stream()
                    .anyMatch(p -> p.getInvestmentId().equals(transaction.getInvestmentId()) &&
                                   p.getQuantity().compareTo(transaction.getQuantity()) >= 0);
            
            if (!hasPosition && transaction.getInvestmentId() != null) {
                errors.add("Insufficient position for sell transaction");
            }
        }
    }

    private void validateAmounts(Transaction transaction, List<String> errors, List<String> warnings) {
        BigDecimal amount = transaction.getAmount();
        
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Transaction amount must be positive");
        }
        
        if (transaction.getQuantity() != null && transaction.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Transaction quantity must be positive");
        }
        
        if (amount != null && amount.compareTo(new BigDecimal("10000000")) > 0) {
            warnings.add("Large transaction amount - requires additional approval");
        }
    }

    private void validateBusinessRules(Transaction transaction, List<String> errors, List<String> warnings) {
        if (transaction.getTransactionDate() != null && 
            transaction.getTransactionDate().isAfter(LocalDate.now())) {
            warnings.add("Future dated transaction");
        }
        
        if (transaction.getCurrency() == null || transaction.getCurrency().isBlank()) {
            transaction.setCurrency("USD");
            warnings.add("Currency defaulted to USD");
        }
    }

    private int calculateReturnCode(List<String> errors, List<String> warnings) {
        if (!errors.isEmpty()) {
            return 8;
        } else if (!warnings.isEmpty()) {
            return 4;
        }
        return 0;
    }

    private int calculateBatchReturnCode(int errorCount, int warningCount) {
        if (errorCount > 0) {
            return 8;
        } else if (warningCount > 0) {
            return 4;
        }
        return 0;
    }

    public static class ValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private int returnCode;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public int getReturnCode() { return returnCode; }
        public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    }

    public static class BatchValidationResult {
        private int totalProcessed;
        private int successCount;
        private int errorCount;
        private int warningCount;
        private int returnCode;
        private List<ValidationResult> results = new ArrayList<>();

        public int getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public int getWarningCount() { return warningCount; }
        public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
        public int getReturnCode() { return returnCode; }
        public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
        public List<ValidationResult> getResults() { return results; }
        public void setResults(List<ValidationResult> results) { this.results = results; }
    }
}
