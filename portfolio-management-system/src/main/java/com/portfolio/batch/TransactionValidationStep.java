package com.portfolio.batch;

import com.portfolio.model.PositionRecord;
import com.portfolio.model.TransactionRecord;
import com.portfolio.support.BatchExceptions;
import com.portfolio.support.Db2StatisticsService;
import com.portfolio.support.ErrorLoggingService;
import com.portfolio.support.PositionRecordRepository;
import com.portfolio.support.TransactionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Transaction Validation Step.
 * Migrated from COBOL TRNVAL00.
 * Validates transaction records: buy, sell, transfer, fee types.
 * Writes validated records to POSITION_MASTER table.
 * Maps exit status to RC 0/4/8/12.
 */
@Component
public class TransactionValidationStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(TransactionValidationStep.class);

    private final TransactionRecordRepository transactionRepository;
    private final PositionRecordRepository positionRepository;
    private final ErrorLoggingService errorLoggingService;
    private final Db2StatisticsService statisticsService;

    private int recordsProcessed = 0;
    private int recordsError = 0;
    private int recordsWarning = 0;

    public TransactionValidationStep(
            TransactionRecordRepository transactionRepository,
            PositionRecordRepository positionRepository,
            ErrorLoggingService errorLoggingService,
            Db2StatisticsService statisticsService) {
        this.transactionRepository = transactionRepository;
        this.positionRepository = positionRepository;
        this.errorLoggingService = errorLoggingService;
        this.statisticsService = statisticsService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Starting Transaction Validation Step (TRNVAL00)");

        // Reset counters for each execution (singleton component reused across runs)
        recordsProcessed = 0;
        recordsError = 0;
        recordsWarning = 0;

        // Query for pending transactions in the daily window
        List<TransactionRecord> transactions =
                transactionRepository.findByPortfolioIdAndTransactionDateBetween(
                        null, LocalDate.now().minusDays(1), LocalDate.now());

        // If no pending transactions in daily window, fall back to all transactions
        if (transactions.isEmpty()) {
            transactions = transactionRepository.findAll();
        }
        log.info("Found {} transactions to validate", transactions.size());

        for (TransactionRecord txn : transactions) {
            try {
                validateTransaction(txn);
                processValidatedTransaction(txn);
                recordsProcessed++;
                statisticsService.recordQuery();
            } catch (BatchExceptions.BatchWarningException e) {
                recordsWarning++;
                errorLoggingService.logWarning("TRNVAL00", "VAL4",
                        e.getMessage(), "BATCH");
            } catch (Exception e) {
                recordsError++;
                errorLoggingService.logApplicationError("TRNVAL00", "VAL8",
                        "Validation failed: " + e.getMessage(), txn.getTransactionId(), "BATCH");
            }
        }

        // Determine return code
        int returnCode = determineReturnCode();
        contribution.setExitStatus(new ExitStatus("RC_" + returnCode));

        log.info("Transaction Validation complete: processed={}, warnings={}, errors={}, RC={}",
                recordsProcessed, recordsWarning, recordsError, returnCode);

        return RepeatStatus.FINISHED;
    }

    /**
     * Validates a single transaction.
     * Checks: valid type, positive quantity/price, valid currency.
     */
    private void validateTransaction(TransactionRecord txn) {
        // Validate transaction type (BU, SL, TR, FE)
        String type = txn.getTransactionType();
        if (!TransactionRecord.TYPE_BUY.equals(type) &&
            !TransactionRecord.TYPE_SELL.equals(type) &&
            !TransactionRecord.TYPE_TRANSFER.equals(type) &&
            !TransactionRecord.TYPE_FEE.equals(type)) {
            throw new BatchExceptions.BatchErrorException(
                    "Invalid transaction type: " + type + " for txn " + txn.getTransactionId());
        }

        // Validate quantity
        if (txn.getQuantity() == null || txn.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            if (!TransactionRecord.TYPE_FEE.equals(type)) {
                throw new BatchExceptions.BatchWarningException(
                        "Non-positive quantity for txn " + txn.getTransactionId());
            }
        }

        // Validate price
        if (txn.getPrice() == null || txn.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BatchExceptions.BatchWarningException(
                    "Negative price for txn " + txn.getTransactionId());
        }

        // Validate portfolio exists
        if (txn.getPortfolioId() == null || txn.getPortfolioId().isBlank()) {
            throw new BatchExceptions.BatchErrorException(
                    "Missing portfolio ID for txn " + txn.getTransactionId());
        }
    }

    /**
     * Process a validated transaction: create/update position in POSITION_MASTER.
     */
    private void processValidatedTransaction(TransactionRecord txn) {
        List<PositionRecord> existing = positionRepository
                .findByPortfolioIdAndStatus(txn.getPortfolioId(), PositionRecord.STATUS_ACTIVE);

        PositionRecord position = existing.stream()
                .filter(p -> p.getSymbolId().trim().equals(txn.getInvestmentId().trim()))
                .findFirst()
                .orElse(null);

        if (position == null) {
            // Create new position
            position = new PositionRecord();
            position.setPortfolioId(txn.getPortfolioId());
            position.setSymbolId(txn.getInvestmentId());
            position.setPositionDate(txn.getTransactionDate());
            position.setQuantity(BigDecimal.ZERO);
            position.setCostBasis(BigDecimal.ZERO);
            position.setMarketValue(BigDecimal.ZERO);
            position.setCurrencyCode(txn.getCurrencyCode());
            position.setStatus(PositionRecord.STATUS_ACTIVE);
        }

        // Update position based on transaction type
        if (txn.isBuy()) {
            position.setQuantity(position.getQuantity().add(txn.getQuantity()));
            position.setCostBasis(position.getCostBasis().add(txn.getAmount()));
        } else if (txn.isSell()) {
            position.setQuantity(position.getQuantity().subtract(txn.getQuantity()));
        } else if (txn.isTransfer()) {
            position.setQuantity(position.getQuantity().add(txn.getQuantity()));
        } else if (txn.isFee()) {
            position.setCostBasis(position.getCostBasis().add(txn.getAmount()));
        }

        position.setMarketValue(position.getQuantity().multiply(txn.getPrice()));
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("BATCH");

        positionRepository.save(position);
        statisticsService.recordInsert();
    }

    private int determineReturnCode() {
        if (recordsError > 0) return BatchExceptions.RC_ERROR;
        if (recordsWarning > 0) return BatchExceptions.RC_WARNING;
        return BatchExceptions.RC_SUCCESS;
    }

    // For testing
    public int getRecordsProcessed() { return recordsProcessed; }
    public int getRecordsError() { return recordsError; }
    public int getRecordsWarning() { return recordsWarning; }
}
