package com.portfolio.batch;

import com.portfolio.model.PositionHistory;
import com.portfolio.model.TransactionHistory;
import com.portfolio.service.AuditService;
import com.portfolio.service.HistoryLoadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * History Load Step - ItemProcessor.
 * Replaces: HISTLD00.cbl as a Spring Batch processing step.
 *
 * Loads processed transactions into history tables.
 * Implements commit-interval logic via Spring Batch chunk configuration.
 */
@Component
public class HistoryLoadStep implements ItemProcessor<TransactionHistory, PositionHistory> {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadStep.class);

    private final HistoryLoadService historyLoadService;
    private final AuditService auditService;

    public HistoryLoadStep(HistoryLoadService historyLoadService,
                            AuditService auditService) {
        this.historyLoadService = historyLoadService;
        this.auditService = auditService;
    }

    @Override
    public PositionHistory process(TransactionHistory transaction) {
        try {
            PositionHistory history = historyLoadService
                    .loadTransactionToHistory(transaction, "BATCH");

            auditService.logTransaction(
                    transaction.getTransactionId(), "HISTLD", "SUCC",
                    "BATCH", transaction.getPortfolioId());

            return history;
        } catch (Exception e) {
            log.error("History load failed for transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage());
            auditService.logTransaction(
                    transaction.getTransactionId(), "HISTLD", "FAIL",
                    "BATCH", transaction.getPortfolioId());
            return null;
        }
    }
}
