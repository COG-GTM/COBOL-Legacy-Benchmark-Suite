package com.portfolio.batch.trnval.validation;

import com.portfolio.batch.trnval.error.ValidationError;
import com.portfolio.batch.trnval.error.ValidationError.ErrorSeverity;
import com.portfolio.batch.trnval.model.TransactionRecord;
import com.portfolio.batch.trnval.model.TransactionRecord.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Transaction Validator - Implements validation rules from COBOL TRNVAL00
 * 
 * Validation Rules (from data-dictionary.md):
 * - Portfolio ID validation (8 characters, alphanumeric)
 * - Investment ID validation (10 characters, alphanumeric)
 * - Transaction Type validation (BU, SL, TR, FE)
 * - Transaction Date must not be future date
 * - Share Quantity must not be zero for BUY/SELL
 * - Amount must be non-zero for FEE
 * - Price must be greater than zero for BUY/SELL
 * - Amount range checks
 * - Data integrity verification
 */
public class TransactionValidator {
    
    private static final Pattern PORTFOLIO_ID_PATTERN = Pattern.compile("^[A-Z0-9]{8}$");
    private static final Pattern INVESTMENT_ID_PATTERN = Pattern.compile("^[A-Z0-9]{10}$");
    private static final Pattern SEQUENCE_NO_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("-99999999.99");
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("99999999.999");
    private static final BigDecimal MIN_QUANTITY = new BigDecimal("-99999999.999");
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999.9999");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    
    private final Set<String> processedTransactionKeys;
    
    public TransactionValidator() {
        this.processedTransactionKeys = new HashSet<>();
    }
    
    /**
     * Validates a transaction record according to COBOL TRNVAL00 business rules
     * 
     * @param transaction The transaction record to validate
     * @return List of validation errors (empty if valid)
     */
    public List<ValidationError> validate(TransactionRecord transaction) {
        List<ValidationError> errors = new ArrayList<>();
        
        validatePortfolioId(transaction, errors);
        validateInvestmentId(transaction, errors);
        validateTransactionType(transaction, errors);
        validateTransactionDate(transaction, errors);
        validateTransactionTime(transaction, errors);
        validateSequenceNo(transaction, errors);
        validateQuantity(transaction, errors);
        validatePrice(transaction, errors);
        validateAmount(transaction, errors);
        validateCurrency(transaction, errors);
        validateStatus(transaction, errors);
        validateBusinessRules(transaction, errors);
        validateDuplicateTransaction(transaction, errors);
        
        return errors;
    }
    
    private void validatePortfolioId(TransactionRecord transaction, List<ValidationError> errors) {
        String portfolioId = transaction.getPortfolioId();
        
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E001")
                    .errorMessage("Portfolio ID is required")
                    .fieldName("PORTFOLIO-ID")
                    .fieldValue(portfolioId)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        if (!PORTFOLIO_ID_PATTERN.matcher(portfolioId.trim()).matches()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E001")
                    .errorMessage("Invalid Portfolio ID format (must be 8 alphanumeric characters)")
                    .fieldName("PORTFOLIO-ID")
                    .fieldValue(portfolioId)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateInvestmentId(TransactionRecord transaction, List<ValidationError> errors) {
        String investmentId = transaction.getInvestmentId();
        
        if (investmentId == null || investmentId.trim().isEmpty()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E002")
                    .errorMessage("Investment ID is required")
                    .fieldName("INVESTMENT-ID")
                    .fieldValue(investmentId)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        if (!INVESTMENT_ID_PATTERN.matcher(investmentId.trim()).matches()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E002")
                    .errorMessage("Invalid Investment ID format (must be 10 alphanumeric characters)")
                    .fieldName("INVESTMENT-ID")
                    .fieldValue(investmentId)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateTransactionType(TransactionRecord transaction, List<ValidationError> errors) {
        TransactionType type = transaction.getType();
        
        if (type == null) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E003")
                    .errorMessage("Invalid Transaction Type (must be BU, SL, TR, or FE)")
                    .fieldName("TRANSACTION-TYPE")
                    .fieldValue("null")
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateTransactionDate(TransactionRecord transaction, List<ValidationError> errors) {
        String dateStr = transaction.getDate();
        
        if (dateStr == null || dateStr.trim().isEmpty()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E005")
                    .errorMessage("Transaction Date is required")
                    .fieldName("TRANSACTION-DATE")
                    .fieldValue(dateStr)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        try {
            LocalDate transDate = LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
            LocalDate today = LocalDate.now();
            
            if (transDate.isAfter(today)) {
                errors.add(new ValidationError.Builder()
                        .severity(ErrorSeverity.ERROR)
                        .errorCode("E005")
                        .errorMessage("Transaction Date cannot be in the future")
                        .fieldName("TRANSACTION-DATE")
                        .fieldValue(dateStr)
                        .lineNumber(transaction.getLineNumber())
                        .transactionKey(transaction.getTransactionKey())
                        .build());
            }
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E005")
                    .errorMessage("Invalid Transaction Date format (must be YYYYMMDD)")
                    .fieldName("TRANSACTION-DATE")
                    .fieldValue(dateStr)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateTransactionTime(TransactionRecord transaction, List<ValidationError> errors) {
        String timeStr = transaction.getTime();
        
        if (timeStr == null || timeStr.trim().isEmpty()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E006")
                    .errorMessage("Transaction Time is required")
                    .fieldName("TRANSACTION-TIME")
                    .fieldValue(timeStr)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        try {
            TIME_FORMATTER.parse(timeStr.trim());
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E006")
                    .errorMessage("Invalid Transaction Time format (must be HHMMSS)")
                    .fieldName("TRANSACTION-TIME")
                    .fieldValue(timeStr)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateSequenceNo(TransactionRecord transaction, List<ValidationError> errors) {
        String sequenceNo = transaction.getSequenceNo();
        
        if (sequenceNo == null || sequenceNo.trim().isEmpty()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E007")
                    .errorMessage("Sequence Number is required")
                    .fieldName("SEQUENCE-NO")
                    .fieldValue(sequenceNo)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        if (!SEQUENCE_NO_PATTERN.matcher(sequenceNo.trim()).matches()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E007")
                    .errorMessage("Invalid Sequence Number format (must be 6 digits)")
                    .fieldName("SEQUENCE-NO")
                    .fieldValue(sequenceNo)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateQuantity(TransactionRecord transaction, List<ValidationError> errors) {
        BigDecimal quantity = transaction.getQuantity();
        
        if (quantity == null) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E008")
                    .errorMessage("Quantity is required")
                    .fieldName("QUANTITY")
                    .fieldValue("null")
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        if (quantity.compareTo(MIN_QUANTITY) < 0 || quantity.compareTo(MAX_QUANTITY) > 0) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E008")
                    .errorMessage("Quantity out of valid range")
                    .fieldName("QUANTITY")
                    .fieldValue(quantity.toString())
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validatePrice(TransactionRecord transaction, List<ValidationError> errors) {
        BigDecimal price = transaction.getPrice();
        
        if (price == null) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E009")
                    .errorMessage("Price is required")
                    .fieldName("PRICE")
                    .fieldValue("null")
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        if (price.compareTo(ZERO) < 0 || price.compareTo(MAX_PRICE) > 0) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E009")
                    .errorMessage("Price out of valid range")
                    .fieldName("PRICE")
                    .fieldValue(price.toString())
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateAmount(TransactionRecord transaction, List<ValidationError> errors) {
        BigDecimal amount = transaction.getAmount();
        
        if (amount == null) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E010")
                    .errorMessage("Amount is required")
                    .fieldName("AMOUNT")
                    .fieldValue("null")
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E010")
                    .errorMessage("Amount out of valid range")
                    .fieldName("AMOUNT")
                    .fieldValue(amount.toString())
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
        
        if (amount.compareTo(ZERO) == 0) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.WARNING)
                    .errorCode("W001")
                    .errorMessage("Zero Dollar Transaction")
                    .fieldName("AMOUNT")
                    .fieldValue(amount.toString())
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateCurrency(TransactionRecord transaction, List<ValidationError> errors) {
        String currency = transaction.getCurrency();
        
        if (currency == null || currency.trim().isEmpty()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E011")
                    .errorMessage("Currency is required")
                    .fieldName("CURRENCY")
                    .fieldValue(currency)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
            return;
        }
        
        if (!CURRENCY_PATTERN.matcher(currency.trim()).matches()) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E011")
                    .errorMessage("Invalid Currency format (must be 3 letter code)")
                    .fieldName("CURRENCY")
                    .fieldValue(currency)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateStatus(TransactionRecord transaction, List<ValidationError> errors) {
        if (transaction.getStatus() == null) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.ERROR)
                    .errorCode("E012")
                    .errorMessage("Invalid Transaction Status")
                    .fieldName("STATUS")
                    .fieldValue("null")
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(transaction.getTransactionKey())
                    .build());
        }
    }
    
    private void validateBusinessRules(TransactionRecord transaction, List<ValidationError> errors) {
        TransactionType type = transaction.getType();
        BigDecimal quantity = transaction.getQuantity();
        BigDecimal price = transaction.getPrice();
        BigDecimal amount = transaction.getAmount();
        
        if (type == null || quantity == null || price == null || amount == null) {
            return;
        }
        
        if (type == TransactionType.BUY || type == TransactionType.SELL) {
            if (quantity.compareTo(ZERO) == 0) {
                errors.add(new ValidationError.Builder()
                        .severity(ErrorSeverity.ERROR)
                        .errorCode("E013")
                        .errorMessage("Share Quantity must not be zero for BUY/SELL transactions")
                        .fieldName("QUANTITY")
                        .fieldValue(quantity.toString())
                        .lineNumber(transaction.getLineNumber())
                        .transactionKey(transaction.getTransactionKey())
                        .build());
            }
            
            if (price.compareTo(ZERO) <= 0) {
                errors.add(new ValidationError.Builder()
                        .severity(ErrorSeverity.ERROR)
                        .errorCode("E014")
                        .errorMessage("Price must be greater than zero for BUY/SELL transactions")
                        .fieldName("PRICE")
                        .fieldValue(price.toString())
                        .lineNumber(transaction.getLineNumber())
                        .transactionKey(transaction.getTransactionKey())
                        .build());
            }
        }
        
        if (type == TransactionType.FEE) {
            if (amount.compareTo(ZERO) == 0) {
                errors.add(new ValidationError.Builder()
                        .severity(ErrorSeverity.ERROR)
                        .errorCode("E015")
                        .errorMessage("Amount must be non-zero for FEE transactions")
                        .fieldName("AMOUNT")
                        .fieldValue(amount.toString())
                        .lineNumber(transaction.getLineNumber())
                        .transactionKey(transaction.getTransactionKey())
                        .build());
            }
        }
    }
    
    private void validateDuplicateTransaction(TransactionRecord transaction, List<ValidationError> errors) {
        String key = transaction.getTransactionKey();
        
        if (processedTransactionKeys.contains(key)) {
            errors.add(new ValidationError.Builder()
                    .severity(ErrorSeverity.WARNING)
                    .errorCode("W002")
                    .errorMessage("Duplicate Transaction ID")
                    .fieldName("TRANSACTION-KEY")
                    .fieldValue(key)
                    .lineNumber(transaction.getLineNumber())
                    .transactionKey(key)
                    .build());
        } else {
            processedTransactionKeys.add(key);
        }
    }
    
    public void reset() {
        processedTransactionKeys.clear();
    }
}
