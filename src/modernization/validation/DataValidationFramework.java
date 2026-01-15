package com.portfolio.modernization.validation;

import com.portfolio.modernization.entity.HistoryRecord;
import com.portfolio.modernization.entity.PositionRecord;
import com.portfolio.modernization.entity.TransactionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Data Validation Framework
 * 
 * Provides comprehensive validation for migrated data from VSAM to relational database.
 * Implements validation rules to ensure data integrity during Phase 1 migration.
 * 
 * Validation Categories:
 * 1. Schema Validation - Data type conversions from COBOL to Java/SQL
 * 2. Business Rule Validation - Financial calculation accuracy
 * 3. Referential Integrity - Foreign key relationships
 * 4. Data Quality Metrics - Baseline quality measures
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Component
public class DataValidationFramework {

    private static final Logger logger = LoggerFactory.getLogger(DataValidationFramework.class);

    /**
     * Validation result containing all validation outcomes
     */
    public static class ValidationResult {
        private final String entityType;
        private final String entityId;
        private final boolean valid;
        private final List<ValidationError> errors;
        private final List<ValidationWarning> warnings;
        private final Map<String, Object> metrics;
        private final LocalDateTime validationTimestamp;

        public ValidationResult(String entityType, String entityId, boolean valid,
                               List<ValidationError> errors, List<ValidationWarning> warnings,
                               Map<String, Object> metrics) {
            this.entityType = entityType;
            this.entityId = entityId;
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.metrics = metrics != null ? metrics : new HashMap<>();
            this.validationTimestamp = LocalDateTime.now();
        }

        public String getEntityType() { return entityType; }
        public String getEntityId() { return entityId; }
        public boolean isValid() { return valid; }
        public List<ValidationError> getErrors() { return errors; }
        public List<ValidationWarning> getWarnings() { return warnings; }
        public Map<String, Object> getMetrics() { return metrics; }
        public LocalDateTime getValidationTimestamp() { return validationTimestamp; }

        @Override
        public String toString() {
            return String.format("ValidationResult{entityType='%s', entityId='%s', valid=%s, errors=%d, warnings=%d}",
                    entityType, entityId, valid, errors.size(), warnings.size());
        }
    }

    /**
     * Validation error details
     */
    public static class ValidationError {
        private final String field;
        private final String errorCode;
        private final String message;
        private final Object actualValue;
        private final Object expectedValue;

        public ValidationError(String field, String errorCode, String message,
                              Object actualValue, Object expectedValue) {
            this.field = field;
            this.errorCode = errorCode;
            this.message = message;
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
        }

        public String getField() { return field; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public Object getActualValue() { return actualValue; }
        public Object getExpectedValue() { return expectedValue; }

        @Override
        public String toString() {
            return String.format("ValidationError{field='%s', errorCode='%s', message='%s'}",
                    field, errorCode, message);
        }
    }

    /**
     * Validation warning details
     */
    public static class ValidationWarning {
        private final String field;
        private final String warningCode;
        private final String message;

        public ValidationWarning(String field, String warningCode, String message) {
            this.field = field;
            this.warningCode = warningCode;
            this.message = message;
        }

        public String getField() { return field; }
        public String getWarningCode() { return warningCode; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return String.format("ValidationWarning{field='%s', warningCode='%s', message='%s'}",
                    field, warningCode, message);
        }
    }

    /**
     * Validates a PositionRecord for schema compliance and business rules.
     * 
     * @param position the position to validate
     * @return validation result
     */
    public ValidationResult validatePosition(PositionRecord position) {
        logger.debug("Validating position: {}", position != null ? position.getPortfolioId() : "null");
        
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        Map<String, Object> metrics = new HashMap<>();

        if (position == null) {
            errors.add(new ValidationError("position", "NULL_ENTITY", 
                    "Position record is null", null, "non-null"));
            return new ValidationResult("Position", "unknown", false, errors, warnings, metrics);
        }

        validatePositionSchema(position, errors, warnings);
        
        validatePositionBusinessRules(position, errors, warnings);
        
        calculatePositionMetrics(position, metrics);

        boolean isValid = errors.isEmpty();
        return new ValidationResult("Position", position.getPortfolioId(), isValid, errors, warnings, metrics);
    }

    /**
     * Validates position schema (data type conversions from COBOL).
     */
    private void validatePositionSchema(PositionRecord position, 
                                        List<ValidationError> errors,
                                        List<ValidationWarning> warnings) {
        if (position.getPortfolioId() == null || position.getPortfolioId().trim().isEmpty()) {
            errors.add(new ValidationError("portfolioId", "REQUIRED_FIELD",
                    "Portfolio ID is required (POS-PORTFOLIO-ID)", null, "non-empty string"));
        } else if (position.getPortfolioId().length() > 20) {
            errors.add(new ValidationError("portfolioId", "FIELD_LENGTH",
                    "Portfolio ID exceeds maximum length", position.getPortfolioId().length(), 20));
        }

        if (position.getAccountNumber() == null || position.getAccountNumber().trim().isEmpty()) {
            errors.add(new ValidationError("accountNumber", "REQUIRED_FIELD",
                    "Account number is required", null, "non-empty string"));
        } else if (position.getAccountNumber().length() > 15) {
            errors.add(new ValidationError("accountNumber", "FIELD_LENGTH",
                    "Account number exceeds maximum length", position.getAccountNumber().length(), 15));
        }

        if (position.getFundId() == null || position.getFundId().trim().isEmpty()) {
            errors.add(new ValidationError("fundId", "REQUIRED_FIELD",
                    "Fund ID is required (POS-INVESTMENT-ID)", null, "non-empty string"));
        } else if (position.getFundId().length() > 10) {
            errors.add(new ValidationError("fundId", "FIELD_LENGTH",
                    "Fund ID exceeds maximum length (POS-INVESTMENT-ID X(10))", 
                    position.getFundId().length(), 10));
        }

        validateDecimalPrecision(position.getUnits(), "units", 11, 4, errors, warnings);
        validateDecimalPrecision(position.getCostBasis(), "costBasis", 13, 2, errors, warnings);
        validateDecimalPrecision(position.getMarketValue(), "marketValue", 13, 2, errors, warnings);

        if (position.getCurrencyCode() == null || position.getCurrencyCode().length() != 3) {
            errors.add(new ValidationError("currencyCode", "INVALID_FORMAT",
                    "Currency code must be 3 characters (POS-CURRENCY X(03))",
                    position.getCurrencyCode(), "3-character code"));
        }

        if (position.getStatus() == null || position.getStatus().length() != 1) {
            errors.add(new ValidationError("status", "INVALID_FORMAT",
                    "Status must be 1 character (POS-STATUS X(01))",
                    position.getStatus(), "1-character code"));
        } else if (!position.isValidStatus()) {
            errors.add(new ValidationError("status", "INVALID_VALUE",
                    "Status must be A, C, or P (from 88-level conditions)",
                    position.getStatus(), "A, C, or P"));
        }
    }

    /**
     * Validates position business rules.
     */
    private void validatePositionBusinessRules(PositionRecord position,
                                               List<ValidationError> errors,
                                               List<ValidationWarning> warnings) {
        if (position.getUnits() != null && position.getUnits().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new ValidationError("units", "BUSINESS_RULE",
                    "Units cannot be negative", position.getUnits(), ">= 0"));
        }

        if (position.getCostBasis() != null && position.getMarketValue() != null) {
            BigDecimal gainLoss = position.calculateUnrealizedGainLoss();
            BigDecimal gainLossPercent = position.calculateUnrealizedGainLossPercent();
            
            if (gainLossPercent.abs().compareTo(new BigDecimal("50")) > 0) {
                warnings.add(new ValidationWarning("marketValue", "HIGH_VARIANCE",
                        String.format("Unrealized gain/loss exceeds 50%%: %.2f%%", gainLossPercent)));
            }
        }

        if (position.isActive() && (position.getUnits() == null || 
            position.getUnits().compareTo(BigDecimal.ZERO) == 0)) {
            warnings.add(new ValidationWarning("status", "INCONSISTENT_STATE",
                    "Active position has zero or null units"));
        }

        if (position.isClosed() && position.getUnits() != null && 
            position.getUnits().compareTo(BigDecimal.ZERO) > 0) {
            warnings.add(new ValidationWarning("status", "INCONSISTENT_STATE",
                    "Closed position has non-zero units"));
        }
    }

    /**
     * Calculates position data quality metrics.
     */
    private void calculatePositionMetrics(PositionRecord position, Map<String, Object> metrics) {
        int completeness = 0;
        int totalFields = 8;
        
        if (position.getPortfolioId() != null && !position.getPortfolioId().trim().isEmpty()) completeness++;
        if (position.getAccountNumber() != null && !position.getAccountNumber().trim().isEmpty()) completeness++;
        if (position.getFundId() != null && !position.getFundId().trim().isEmpty()) completeness++;
        if (position.getUnits() != null) completeness++;
        if (position.getCostBasis() != null) completeness++;
        if (position.getMarketValue() != null) completeness++;
        if (position.getCurrencyCode() != null) completeness++;
        if (position.getStatus() != null) completeness++;
        
        metrics.put("completenessScore", (double) completeness / totalFields * 100);
        metrics.put("hasVsamMigrationData", position.getVsamMigrationDate() != null);
        metrics.put("hasHoldings", position.hasHoldings());
        metrics.put("isActive", position.isActive());
    }

    /**
     * Validates a TransactionRecord for schema compliance and business rules.
     * 
     * @param transaction the transaction to validate
     * @return validation result
     */
    public ValidationResult validateTransaction(TransactionRecord transaction) {
        logger.debug("Validating transaction: {}", 
                transaction != null ? transaction.getTransactionId() : "null");
        
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        Map<String, Object> metrics = new HashMap<>();

        if (transaction == null) {
            errors.add(new ValidationError("transaction", "NULL_ENTITY",
                    "Transaction record is null", null, "non-null"));
            return new ValidationResult("Transaction", "unknown", false, errors, warnings, metrics);
        }

        validateTransactionSchema(transaction, errors, warnings);
        
        validateTransactionBusinessRules(transaction, errors, warnings);
        
        calculateTransactionMetrics(transaction, metrics);

        boolean isValid = errors.isEmpty();
        return new ValidationResult("Transaction", transaction.getTransactionId(), 
                isValid, errors, warnings, metrics);
    }

    /**
     * Validates transaction schema (data type conversions from COBOL).
     */
    private void validateTransactionSchema(TransactionRecord transaction,
                                           List<ValidationError> errors,
                                           List<ValidationWarning> warnings) {
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().trim().isEmpty()) {
            errors.add(new ValidationError("portfolioId", "REQUIRED_FIELD",
                    "Portfolio ID is required (TRN-PORTFOLIO-ID)", null, "non-empty string"));
        } else if (transaction.getPortfolioId().length() > 20) {
            errors.add(new ValidationError("portfolioId", "FIELD_LENGTH",
                    "Portfolio ID exceeds maximum length", transaction.getPortfolioId().length(), 20));
        }

        if (transaction.getTransactionDate() == null) {
            errors.add(new ValidationError("transactionDate", "REQUIRED_FIELD",
                    "Transaction date is required (TRN-DATE)", null, "valid date"));
        }

        if (transaction.getTransactionType() == null || transaction.getTransactionType().trim().isEmpty()) {
            errors.add(new ValidationError("transactionType", "REQUIRED_FIELD",
                    "Transaction type is required (TRN-TYPE)", null, "non-empty string"));
        } else if (!transaction.isValidTransactionType()) {
            errors.add(new ValidationError("transactionType", "INVALID_VALUE",
                    "Invalid transaction type (must be BUY/SELL/TRANSFER/FEE or BU/SL/TR/FE)",
                    transaction.getTransactionType(), "valid type code"));
        }

        if (transaction.getAmount() == null) {
            errors.add(new ValidationError("amount", "REQUIRED_FIELD",
                    "Amount is required (TRN-AMOUNT)", null, "valid decimal"));
        }

        validateDecimalPrecision(transaction.getAmount(), "amount", 13, 2, errors, warnings);
        validateDecimalPrecision(transaction.getUnits(), "units", 11, 4, errors, warnings);
        validateDecimalPrecision(transaction.getPrice(), "price", 11, 4, errors, warnings);

        if (transaction.getCurrencyCode() == null || transaction.getCurrencyCode().length() != 3) {
            errors.add(new ValidationError("currencyCode", "INVALID_FORMAT",
                    "Currency code must be 3 characters (TRN-CURRENCY X(03))",
                    transaction.getCurrencyCode(), "3-character code"));
        }

        if (transaction.getStatus() == null || transaction.getStatus().length() != 1) {
            errors.add(new ValidationError("status", "INVALID_FORMAT",
                    "Status must be 1 character (TRN-STATUS X(01))",
                    transaction.getStatus(), "1-character code"));
        } else if (!transaction.isValidStatus()) {
            errors.add(new ValidationError("status", "INVALID_VALUE",
                    "Status must be P, D, F, or R (from 88-level conditions)",
                    transaction.getStatus(), "P, D, F, or R"));
        }
    }

    /**
     * Validates transaction business rules.
     */
    private void validateTransactionBusinessRules(TransactionRecord transaction,
                                                  List<ValidationError> errors,
                                                  List<ValidationWarning> warnings) {
        if (transaction.getAmount() != null && transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new ValidationError("amount", "BUSINESS_RULE",
                    "Amount cannot be negative", transaction.getAmount(), ">= 0"));
        }

        if (transaction.isBuyTransaction() || transaction.isSellTransaction()) {
            if (transaction.getUnits() == null || transaction.getUnits().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new ValidationError("units", "BUSINESS_RULE",
                        "Units must be positive for buy/sell transactions", 
                        transaction.getUnits(), "> 0"));
            }
            if (transaction.getPrice() == null || transaction.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new ValidationError("price", "BUSINESS_RULE",
                        "Price must be positive for buy/sell transactions",
                        transaction.getPrice(), "> 0"));
            }
        }

        if (transaction.getUnits() != null && transaction.getPrice() != null && 
            transaction.getAmount() != null) {
            BigDecimal calculatedAmount = transaction.calculateTotalValue();
            BigDecimal difference = transaction.getAmount().subtract(calculatedAmount).abs();
            BigDecimal tolerance = new BigDecimal("0.01");
            
            if (difference.compareTo(tolerance) > 0) {
                warnings.add(new ValidationWarning("amount", "CALCULATION_MISMATCH",
                        String.format("Amount (%.2f) differs from units * price (%.2f)",
                                transaction.getAmount(), calculatedAmount)));
            }
        }

        if (transaction.getTransactionDate() != null && 
            transaction.getTransactionDate().isAfter(LocalDate.now())) {
            warnings.add(new ValidationWarning("transactionDate", "FUTURE_DATE",
                    "Transaction date is in the future"));
        }
    }

    /**
     * Calculates transaction data quality metrics.
     */
    private void calculateTransactionMetrics(TransactionRecord transaction, Map<String, Object> metrics) {
        int completeness = 0;
        int totalFields = 10;
        
        if (transaction.getTransactionId() != null) completeness++;
        if (transaction.getPortfolioId() != null) completeness++;
        if (transaction.getTransactionDate() != null) completeness++;
        if (transaction.getTransactionTime() != null) completeness++;
        if (transaction.getTransactionType() != null) completeness++;
        if (transaction.getInvestmentId() != null) completeness++;
        if (transaction.getAmount() != null) completeness++;
        if (transaction.getUnits() != null) completeness++;
        if (transaction.getPrice() != null) completeness++;
        if (transaction.getStatus() != null) completeness++;
        
        metrics.put("completenessScore", (double) completeness / totalFields * 100);
        metrics.put("hasVsamMigrationData", transaction.getVsamMigrationDate() != null);
        metrics.put("isPending", transaction.isPending());
        metrics.put("isCompleted", transaction.isCompleted());
    }

    /**
     * Validates a HistoryRecord for schema compliance.
     * 
     * @param history the history record to validate
     * @return validation result
     */
    public ValidationResult validateHistory(HistoryRecord history) {
        logger.debug("Validating history record: {}", 
                history != null ? history.getHistoryId() : "null");
        
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        Map<String, Object> metrics = new HashMap<>();

        if (history == null) {
            errors.add(new ValidationError("history", "NULL_ENTITY",
                    "History record is null", null, "non-null"));
            return new ValidationResult("History", "unknown", false, errors, warnings, metrics);
        }

        if (history.getPortfolioId() == null || history.getPortfolioId().trim().isEmpty()) {
            errors.add(new ValidationError("portfolioId", "REQUIRED_FIELD",
                    "Portfolio ID is required (HIST-PORTFOLIO-ID)", null, "non-empty string"));
        }

        if (history.getHistoryDate() == null) {
            errors.add(new ValidationError("historyDate", "REQUIRED_FIELD",
                    "History date is required (HIST-DATE)", null, "valid date"));
        }

        if (!history.isValidRecordType()) {
            errors.add(new ValidationError("recordType", "INVALID_VALUE",
                    "Invalid record type (must be PT, PS, or TR)",
                    history.getRecordType(), "PT, PS, or TR"));
        }

        if (!history.isValidActionCode()) {
            errors.add(new ValidationError("actionCode", "INVALID_VALUE",
                    "Invalid action code (must be A, C, or D)",
                    history.getActionCode(), "A, C, or D"));
        }

        int completeness = 0;
        int totalFields = 8;
        if (history.getPortfolioId() != null) completeness++;
        if (history.getHistoryDate() != null) completeness++;
        if (history.getHistoryTime() != null) completeness++;
        if (history.getRecordType() != null) completeness++;
        if (history.getActionCode() != null) completeness++;
        if (history.getBeforeImage() != null || history.getAfterImage() != null) completeness++;
        if (history.getProcessUser() != null) completeness++;
        if (history.getProcessDate() != null) completeness++;
        
        metrics.put("completenessScore", (double) completeness / totalFields * 100);
        metrics.put("hasVsamMigrationData", history.getVsamMigrationDate() != null);

        boolean isValid = errors.isEmpty();
        return new ValidationResult("History", 
                history.getHistoryId() != null ? history.getHistoryId().toString() : "new",
                isValid, errors, warnings, metrics);
    }

    /**
     * Validates decimal precision against COBOL COMP-3 field definitions.
     */
    private void validateDecimalPrecision(BigDecimal value, String fieldName,
                                          int maxIntegerDigits, int maxFractionDigits,
                                          List<ValidationError> errors,
                                          List<ValidationWarning> warnings) {
        if (value == null) {
            return;
        }

        int integerDigits = value.precision() - value.scale();
        int fractionDigits = Math.max(0, value.scale());

        if (integerDigits > maxIntegerDigits) {
            errors.add(new ValidationError(fieldName, "PRECISION_OVERFLOW",
                    String.format("Integer digits (%d) exceed COBOL field capacity (%d)",
                            integerDigits, maxIntegerDigits),
                    integerDigits, maxIntegerDigits));
        }

        if (fractionDigits > maxFractionDigits) {
            warnings.add(new ValidationWarning(fieldName, "PRECISION_TRUNCATION",
                    String.format("Fraction digits (%d) exceed COBOL field capacity (%d), may be truncated",
                            fractionDigits, maxFractionDigits)));
        }
    }

    /**
     * Validates referential integrity between positions and transactions.
     * 
     * @param positions list of positions
     * @param transactions list of transactions
     * @return validation result
     */
    public ValidationResult validateReferentialIntegrity(List<PositionRecord> positions,
                                                         List<TransactionRecord> transactions) {
        logger.info("Validating referential integrity");
        
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        Map<String, Object> metrics = new HashMap<>();

        Set<String> positionIds = positions.stream()
                .map(PositionRecord::getPortfolioId)
                .collect(Collectors.toSet());

        int orphanedTransactions = 0;
        for (TransactionRecord transaction : transactions) {
            if (!positionIds.contains(transaction.getPortfolioId())) {
                orphanedTransactions++;
                warnings.add(new ValidationWarning("portfolioId", "ORPHANED_RECORD",
                        String.format("Transaction %s references non-existent position %s",
                                transaction.getTransactionId(), transaction.getPortfolioId())));
            }
        }

        metrics.put("totalPositions", positions.size());
        metrics.put("totalTransactions", transactions.size());
        metrics.put("orphanedTransactions", orphanedTransactions);
        metrics.put("referentialIntegrityScore", 
                transactions.isEmpty() ? 100.0 : 
                (1.0 - (double) orphanedTransactions / transactions.size()) * 100);

        boolean isValid = errors.isEmpty();
        return new ValidationResult("ReferentialIntegrity", "batch", isValid, errors, warnings, metrics);
    }

    /**
     * Generates a comprehensive data quality report.
     * 
     * @param positionResults position validation results
     * @param transactionResults transaction validation results
     * @return data quality report
     */
    public Map<String, Object> generateDataQualityReport(List<ValidationResult> positionResults,
                                                         List<ValidationResult> transactionResults) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportTimestamp", LocalDateTime.now());
        
        Map<String, Object> positionStats = new LinkedHashMap<>();
        positionStats.put("totalRecords", positionResults.size());
        positionStats.put("validRecords", positionResults.stream().filter(ValidationResult::isValid).count());
        positionStats.put("invalidRecords", positionResults.stream().filter(r -> !r.isValid()).count());
        positionStats.put("totalErrors", positionResults.stream().mapToInt(r -> r.getErrors().size()).sum());
        positionStats.put("totalWarnings", positionResults.stream().mapToInt(r -> r.getWarnings().size()).sum());
        positionStats.put("averageCompleteness", positionResults.stream()
                .mapToDouble(r -> (Double) r.getMetrics().getOrDefault("completenessScore", 0.0))
                .average().orElse(0.0));
        report.put("positionStatistics", positionStats);
        
        Map<String, Object> transactionStats = new LinkedHashMap<>();
        transactionStats.put("totalRecords", transactionResults.size());
        transactionStats.put("validRecords", transactionResults.stream().filter(ValidationResult::isValid).count());
        transactionStats.put("invalidRecords", transactionResults.stream().filter(r -> !r.isValid()).count());
        transactionStats.put("totalErrors", transactionResults.stream().mapToInt(r -> r.getErrors().size()).sum());
        transactionStats.put("totalWarnings", transactionResults.stream().mapToInt(r -> r.getWarnings().size()).sum());
        transactionStats.put("averageCompleteness", transactionResults.stream()
                .mapToDouble(r -> (Double) r.getMetrics().getOrDefault("completenessScore", 0.0))
                .average().orElse(0.0));
        report.put("transactionStatistics", transactionStats);
        
        Map<String, Long> errorsByCode = new HashMap<>();
        positionResults.stream()
                .flatMap(r -> r.getErrors().stream())
                .forEach(e -> errorsByCode.merge(e.getErrorCode(), 1L, Long::sum));
        transactionResults.stream()
                .flatMap(r -> r.getErrors().stream())
                .forEach(e -> errorsByCode.merge(e.getErrorCode(), 1L, Long::sum));
        report.put("errorDistribution", errorsByCode);
        
        double overallQuality = ((long) positionStats.get("validRecords") + 
                                (long) transactionStats.get("validRecords")) * 100.0 /
                               Math.max(1, (int) positionStats.get("totalRecords") + 
                                          (int) transactionStats.get("totalRecords"));
        report.put("overallDataQualityScore", overallQuality);
        
        return report;
    }
}
