package com.portfolio.batch;

import com.portfolio.entity.TransactionHistory;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Transaction Validation Step - replaces TRNVAL00.cbl.
 * Source: src/programs/batch/TRNVAL00.cbl
 *
 * COBOL logic:
 * - P200-VALIDATE-TRANSACTION: Validates each transaction record
 * - P210-VALIDATE-FIELDS: Field-level validation (amount > 0, valid type, etc.)
 * - P220-CHECK-PORTFOLIO: Verifies portfolio exists and is active
 * - P300-UPDATE-STATUS: Sets transaction status to 'P' (Processed) or 'F' (Failed)
 *
 * Return codes: 0 = all valid, 4 = some warnings, 8+ = critical errors
 */
@Component
public class TransactionValidationStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(TransactionValidationStep.class);

    private final TransactionHistoryRepository transactionRepository;

    public TransactionValidationStep(TransactionHistoryRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("TRNVAL00: Starting transaction validation");

        List<TransactionHistory> pendingTransactions = transactionRepository.findByStatus("D");
        int validCount = 0;
        int errorCount = 0;

        for (TransactionHistory txn : pendingTransactions) {
            boolean valid = validateTransaction(txn);
            if (valid) {
                txn.setStatus("P");
                validCount++;
            } else {
                txn.setStatus("F");
                errorCount++;
            }
            transactionRepository.save(txn);
        }

        log.info("TRNVAL00: Completed. Processed={}, Valid={}, Errors={}",
                pendingTransactions.size(), validCount, errorCount);

        contribution.incrementReadCount();

        return RepeatStatus.FINISHED;
    }

    /**
     * Validates a single transaction - replaces P210-VALIDATE-FIELDS.
     */
    private boolean validateTransaction(TransactionHistory txn) {
        if (txn.getTransactionType() == null || txn.getTransactionType().isBlank()) {
            return false;
        }
        if (!List.of("BU", "SL", "TR", "FE").contains(txn.getTransactionType())) {
            return false;
        }
        if (txn.getAmount() == null || txn.getAmount().signum() < 0) {
            return false;
        }
        if (txn.getPortfolioId() == null || txn.getPortfolioId().isBlank()) {
            return false;
        }
        if (txn.getInvestmentId() == null || txn.getInvestmentId().isBlank()) {
            return false;
        }
        return true;
    }
}
