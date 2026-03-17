package com.portfolio.batch;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.TransactionHistory;
import com.portfolio.service.AuditService;
import com.portfolio.service.PositionUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Position Update Step - ItemProcessor.
 * Replaces: POSUPD00.cbl as a Spring Batch processing step.
 *
 * Reads validated transactions and updates investment positions
 * using PositionUpdateService. All financial calculations use BigDecimal.
 */
@Component
public class PositionUpdateStep implements ItemProcessor<TransactionHistory, InvestmentPosition> {

    private static final Logger log = LoggerFactory.getLogger(PositionUpdateStep.class);

    private final PositionUpdateService positionUpdateService;
    private final AuditService auditService;

    public PositionUpdateStep(PositionUpdateService positionUpdateService,
                               AuditService auditService) {
        this.positionUpdateService = positionUpdateService;
        this.auditService = auditService;
    }

    @Override
    public InvestmentPosition process(TransactionHistory transaction) {
        try {
            InvestmentPosition position = positionUpdateService
                    .updatePosition(transaction, "BATCH");

            auditService.logTransaction(
                    transaction.getTransactionId(), "POSUPD", "SUCC",
                    "BATCH", transaction.getPortfolioId());

            return position;
        } catch (Exception e) {
            log.error("Position update failed for transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage());
            auditService.logTransaction(
                    transaction.getTransactionId(), "POSUPD", "FAIL",
                    "BATCH", transaction.getPortfolioId());
            return null;
        }
    }
}
