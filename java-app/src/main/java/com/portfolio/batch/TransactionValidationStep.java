package com.portfolio.batch;

import com.portfolio.model.TransactionHistory;
import com.portfolio.service.AuditService;
import com.portfolio.service.TransactionValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Transaction Validation Step - ItemProcessor.
 * Replaces: TRNVAL00.cbl as a Spring Batch processing step.
 *
 * Reads transactions from staging, validates using TransactionValidationService,
 * and writes validated transactions. Bad records are skipped per skip policy.
 */
@Component
public class TransactionValidationStep implements ItemProcessor<TransactionHistory, TransactionHistory> {

    private static final Logger log = LoggerFactory.getLogger(TransactionValidationStep.class);

    private final TransactionValidationService validationService;
    private final AuditService auditService;

    public TransactionValidationStep(TransactionValidationService validationService,
                                      AuditService auditService) {
        this.validationService = validationService;
        this.auditService = auditService;
    }

    /**
     * Processes (validates) a single transaction.
     * Returns null to filter out invalid transactions (Spring Batch convention).
     */
    @Override
    public TransactionHistory process(TransactionHistory transaction) {
        List<String> errors = validationService.validate(transaction);

        if (errors.isEmpty()) {
            transaction.setStatus("P");
            auditService.logTransaction(
                    transaction.getTransactionId(), "VALIDATE", "SUCC",
                    "BATCH", transaction.getPortfolioId());
            return transaction;
        } else {
            transaction.setStatus("F");
            log.warn("Transaction {} failed validation: {}",
                    transaction.getTransactionId(), errors);
            auditService.logTransaction(
                    transaction.getTransactionId(), "VALIDATE", "FAIL",
                    "BATCH", transaction.getPortfolioId());
            return null; // Filtered out by Spring Batch
        }
    }
}
