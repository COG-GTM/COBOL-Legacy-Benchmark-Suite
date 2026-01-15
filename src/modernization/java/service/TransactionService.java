package com.portfolio.modernization.service;

import com.portfolio.modernization.entity.HistoryRecord;
import com.portfolio.modernization.entity.TransactionRecord;
import com.portfolio.modernization.repository.HistoryRepository;
import com.portfolio.modernization.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Transaction Service
 * 
 * Implements business logic for transaction processing.
 * Modernized from COBOL program: src/programs/batch/TRNVAL00.cbl
 * 
 * Original COBOL business logic preserved:
 * - Transaction validation (P100-VALIDATE-TRANSACTION)
 * - Amount calculation (P200-CALCULATE-AMOUNTS)
 * - Transaction processing (P300-PROCESS-TRANSACTION)
 * - Audit logging (P400-LOG-AUDIT)
 * 
 * @version 1.0
 * @since Phase 1 - Foundation and Data Migration
 */
@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final HistoryRepository historyRepository;
    private final PortfolioService portfolioService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                             HistoryRepository historyRepository,
                             PortfolioService portfolioService) {
        this.transactionRepository = transactionRepository;
        this.historyRepository = historyRepository;
        this.portfolioService = portfolioService;
    }

    /**
     * Validates and processes a transaction.
     * Preserves original business logic from TRNVAL00.
     * 
     * @param transaction the transaction to process
     * @return the processed transaction
     * @throws IllegalArgumentException if validation fails
     */
    public TransactionRecord processTransaction(TransactionRecord transaction) {
        logger.info("Processing transaction: {}", transaction.getTransactionId());
        
        validateTransaction(transaction);
        
        calculateAmounts(transaction);
        
        TransactionRecord savedTransaction = transactionRepository.save(transaction);
        
        logTransactionHistory(savedTransaction, null, captureTransactionState(savedTransaction), "NEW");
        
        logger.info("Transaction processed successfully: {}", savedTransaction.getTransactionId());
        return savedTransaction;
    }

    /**
     * Validates a transaction record.
     * Preserves original business logic from TRNVAL00 P100-VALIDATE-TRANSACTION.
     * 
     * @param transaction the transaction to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateTransaction(TransactionRecord transaction) {
        List<String> errors = new ArrayList<>();
        
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
        
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().trim().isEmpty()) {
            errors.add("Portfolio ID is required");
        }
        
        if (transaction.getTransactionDate() == null) {
            errors.add("Transaction date is required");
        } else if (transaction.getTransactionDate().isAfter(LocalDate.now())) {
            errors.add("Transaction date cannot be in the future");
        }
        
        if (transaction.getTransactionType() == null || transaction.getTransactionType().trim().isEmpty()) {
            errors.add("Transaction type is required");
        } else if (!transaction.isValidTransactionType()) {
            errors.add("Invalid transaction type: " + transaction.getTransactionType());
        }
        
        if (transaction.getAmount() == null) {
            errors.add("Amount is required");
        } else if (transaction.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Amount cannot be negative");
        }
        
        if (transaction.isBuyTransaction() || transaction.isSellTransaction()) {
            if (transaction.getUnits() == null || transaction.getUnits().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Units must be positive for buy/sell transactions");
            }
            if (transaction.getPrice() == null || transaction.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Price must be positive for buy/sell transactions");
            }
        }
        
        if (transaction.getCurrencyCode() == null || transaction.getCurrencyCode().length() != 3) {
            errors.add("Valid 3-character currency code is required");
        }
        
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            logger.error("Transaction validation failed: {}", errorMessage);
            throw new IllegalArgumentException("Transaction validation failed: " + errorMessage);
        }
        
        logger.debug("Transaction validation passed: {}", transaction.getTransactionId());
    }

    /**
     * Calculates transaction amounts.
     * Preserves original business logic from TRNVAL00 P200-CALCULATE-AMOUNTS.
     * 
     * @param transaction the transaction to calculate
     */
    public void calculateAmounts(TransactionRecord transaction) {
        if (transaction.getUnits() != null && transaction.getPrice() != null) {
            BigDecimal calculatedAmount = transaction.calculateTotalValue();
            
            if (transaction.getAmount() == null) {
                transaction.setAmount(calculatedAmount);
            } else {
                BigDecimal difference = transaction.getAmount().subtract(calculatedAmount).abs();
                BigDecimal tolerance = new BigDecimal("0.01");
                
                if (difference.compareTo(tolerance) > 0) {
                    logger.warn("Amount mismatch for transaction {}. Provided: {}, Calculated: {}",
                            transaction.getTransactionId(), transaction.getAmount(), calculatedAmount);
                }
            }
        }
        
        logger.debug("Amounts calculated for transaction: {}", transaction.getTransactionId());
    }

    /**
     * Processes a transaction and updates the associated position.
     * 
     * @param transaction the transaction to process
     * @return the processed transaction
     */
    public TransactionRecord processTransactionWithPositionUpdate(TransactionRecord transaction) {
        logger.info("Processing transaction with position update: {}", transaction.getTransactionId());
        
        TransactionRecord processedTransaction = processTransaction(transaction);
        
        try {
            if (processedTransaction.isBuyTransaction()) {
                portfolioService.processBuyTransaction(
                        processedTransaction.getPortfolioId(), processedTransaction);
            } else if (processedTransaction.isSellTransaction()) {
                portfolioService.processSellTransaction(
                        processedTransaction.getPortfolioId(), processedTransaction);
            }
            
            processedTransaction.markCompleted();
            transactionRepository.save(processedTransaction);
            
        } catch (Exception e) {
            logger.error("Failed to update position for transaction: {}", e.getMessage());
            processedTransaction.markFailed();
            transactionRepository.save(processedTransaction);
            throw e;
        }
        
        return processedTransaction;
    }

    /**
     * Gets a transaction by ID.
     * 
     * @param transactionId the transaction identifier
     * @return optional transaction if found
     */
    @Transactional(readOnly = true)
    public Optional<TransactionRecord> getTransaction(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    /**
     * Gets all transactions for a portfolio.
     * 
     * @param portfolioId the portfolio identifier
     * @return list of transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> getTransactionsByPortfolio(String portfolioId) {
        return transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolioId);
    }

    /**
     * Gets transactions for a portfolio within a date range.
     * 
     * @param portfolioId the portfolio identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> getTransactionsByPortfolioAndDateRange(
            String portfolioId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByPortfolioIdAndDateRange(portfolioId, startDate, endDate);
    }

    /**
     * Gets pending transactions.
     * 
     * @return list of pending transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> getPendingTransactions() {
        return transactionRepository.findPendingTransactions();
    }

    /**
     * Gets failed transactions.
     * 
     * @return list of failed transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionRecord> getFailedTransactions() {
        return transactionRepository.findFailedTransactions();
    }

    /**
     * Marks a transaction as completed.
     * 
     * @param transactionId the transaction identifier
     * @return the updated transaction
     */
    public TransactionRecord completeTransaction(String transactionId) {
        TransactionRecord transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        
        String beforeImage = captureTransactionState(transaction);
        
        transaction.markCompleted();
        TransactionRecord savedTransaction = transactionRepository.save(transaction);
        
        String afterImage = captureTransactionState(savedTransaction);
        logTransactionHistory(savedTransaction, beforeImage, afterImage, "CMP");
        
        return savedTransaction;
    }

    /**
     * Marks a transaction as failed.
     * 
     * @param transactionId the transaction identifier
     * @return the updated transaction
     */
    public TransactionRecord failTransaction(String transactionId) {
        TransactionRecord transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        
        String beforeImage = captureTransactionState(transaction);
        
        transaction.markFailed();
        TransactionRecord savedTransaction = transactionRepository.save(transaction);
        
        String afterImage = captureTransactionState(savedTransaction);
        logTransactionHistory(savedTransaction, beforeImage, afterImage, "FAL");
        
        return savedTransaction;
    }

    /**
     * Reverses a transaction.
     * 
     * @param transactionId the transaction identifier
     * @return the reversed transaction
     */
    public TransactionRecord reverseTransaction(String transactionId) {
        logger.info("Reversing transaction: {}", transactionId);
        
        TransactionRecord transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        
        if (!transaction.isCompleted()) {
            throw new IllegalStateException("Only completed transactions can be reversed");
        }
        
        String beforeImage = captureTransactionState(transaction);
        
        transaction.markReversed();
        TransactionRecord savedTransaction = transactionRepository.save(transaction);
        
        String afterImage = captureTransactionState(savedTransaction);
        logTransactionHistory(savedTransaction, beforeImage, afterImage, "REV");
        
        logger.info("Transaction reversed: {}", transactionId);
        return savedTransaction;
    }

    /**
     * Calculates total transaction amount by type for a portfolio.
     * 
     * @param portfolioId the portfolio identifier
     * @param transactionType the transaction type
     * @return total amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAmountByType(String portfolioId, String transactionType) {
        BigDecimal total = transactionRepository.calculateTotalAmountByPortfolioAndType(
                portfolioId, transactionType);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Gets transaction count by status.
     * 
     * @param status the transaction status
     * @return count of transactions
     */
    @Transactional(readOnly = true)
    public long getTransactionCountByStatus(String status) {
        return transactionRepository.countByStatus(status);
    }

    /**
     * Processes a batch of transactions.
     * 
     * @param transactions list of transactions to process
     * @return list of processed transactions
     */
    public List<TransactionRecord> processBatch(List<TransactionRecord> transactions) {
        logger.info("Processing batch of {} transactions", transactions.size());
        
        List<TransactionRecord> processedTransactions = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (TransactionRecord transaction : transactions) {
            try {
                TransactionRecord processed = processTransaction(transaction);
                processedTransactions.add(processed);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to process transaction: {}", e.getMessage());
                transaction.markFailed();
                transactionRepository.save(transaction);
                processedTransactions.add(transaction);
                failCount++;
            }
        }
        
        logger.info("Batch processing complete. Success: {}, Failed: {}", successCount, failCount);
        return processedTransactions;
    }

    /**
     * Captures the current state of a transaction for history logging.
     * 
     * @param transaction the transaction to capture
     * @return string representation of transaction state
     */
    private String captureTransactionState(TransactionRecord transaction) {
        if (transaction == null) {
            return null;
        }
        return String.format("ID=%s|PORT=%s|DATE=%s|TYPE=%s|AMT=%s|UNITS=%s|PRICE=%s|STATUS=%s",
                transaction.getTransactionId(),
                transaction.getPortfolioId(),
                transaction.getTransactionDate(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getUnits(),
                transaction.getPrice(),
                transaction.getStatus());
    }

    /**
     * Logs transaction history for audit trail.
     * Preserves original business logic from TRNVAL00 P400-LOG-AUDIT.
     * 
     * @param transaction the transaction
     * @param beforeImage state before change
     * @param afterImage state after change
     * @param reasonCode reason for change
     */
    private void logTransactionHistory(TransactionRecord transaction, String beforeImage,
                                       String afterImage, String reasonCode) {
        try {
            String actionCode = beforeImage == null ? HistoryRecord.ACTION_ADD :
                               afterImage == null ? HistoryRecord.ACTION_DELETE :
                               HistoryRecord.ACTION_CHANGE;
            
            HistoryRecord history = new HistoryRecord(
                    transaction.getPortfolioId(),
                    HistoryRecord.RECORD_TYPE_TRANSACTION,
                    actionCode);
            
            history.setBeforeImage(beforeImage);
            history.setAfterImage(afterImage);
            history.setReasonCode(reasonCode);
            history.setProcessUser(transaction.getProcessUser());
            history.setEntityId(transaction.getTransactionId());
            history.setEntityType("TRANSACTION");
            history.generateChangeSummary();
            
            historyRepository.save(history);
            
            logger.debug("Transaction history logged for: {}", transaction.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to log transaction history: {}", e.getMessage());
        }
    }
}
