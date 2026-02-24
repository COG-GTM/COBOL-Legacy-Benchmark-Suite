package com.portfolio.batch;

import com.portfolio.entity.InvestmentPosition;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Position Update Step - replaces POSUPD00.cbl (also referenced as POSUPDT).
 * Source: src/programs/batch/POSUPD00.cbl
 *
 * COBOL logic:
 * - P200-UPDATE-POSITION: Read VSAM position master, update quantity/cost
 * - P210-CALC-BUY: Add quantity, recalculate cost basis for buys
 * - P220-CALC-SELL: Subtract quantity, realize gain/loss for sells
 * - P230-WRITE-POSITION: Write updated position back to VSAM
 */
@Component
public class PositionUpdateStep implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(PositionUpdateStep.class);

    private final TransactionHistoryRepository transactionRepository;
    private final InvestmentPositionRepository positionRepository;

    public PositionUpdateStep(TransactionHistoryRepository transactionRepository,
                              InvestmentPositionRepository positionRepository) {
        this.transactionRepository = transactionRepository;
        this.positionRepository = positionRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("POSUPD00: Starting position update");

        List<TransactionHistory> processedTransactions = transactionRepository.findByStatus("P");
        int updateCount = 0;

        for (TransactionHistory txn : processedTransactions) {
            boolean updated = updatePosition(txn);
            if (updated) {
                // Mark as 'U' (Updated) so HistoryLoadStep can pick it up
                txn.setStatus("U");
                updateCount++;
            } else {
                // Position update was skipped (e.g. Fee/Transfer on non-existent position)
                txn.setStatus("F");
            }
            transactionRepository.save(txn);
        }

        log.info("POSUPD00: Completed. Positions updated={}", updateCount);

        return RepeatStatus.FINISHED;
    }

    /**
     * Update position based on transaction - replaces P200-UPDATE-POSITION.
     */
    private boolean updatePosition(TransactionHistory txn) {
        // Look up existing active position by portfolioId + investmentId (date-independent)
        // This matches the COBOL VSAM keyed lookup which finds the current position record
        List<InvestmentPosition> existing = positionRepository.findActiveByPortfolioAndInvestment(
                txn.getPortfolioId(), txn.getInvestmentId());

        boolean isNewPosition = existing.isEmpty();
        InvestmentPosition position;
        if (!isNewPosition) {
            // Use the most recent active position
            position = existing.get(0);
        } else {
            // Only create new positions for Buy transactions;
            // Sell/Fee/Transfer on non-existent positions is invalid
            if (!"BU".equals(txn.getTransactionType())) {
                log.warn("POSUPD00: Skipping {} transaction for non-existent position: portfolio={}, investment={}",
                        txn.getTransactionType(), txn.getPortfolioId(), txn.getInvestmentId());
                return false;
            }
            position = new InvestmentPosition();
            position.setPortfolioId(txn.getPortfolioId());
            position.setInvestmentId(txn.getInvestmentId());
            position.setPositionDate(txn.getTransactionDate());
            position.setQuantity(BigDecimal.ZERO);
            position.setCostBasis(BigDecimal.ZERO);
            position.setMarketValue(BigDecimal.ZERO);
            position.setCurrencyCode(txn.getCurrencyCode());
            position.setStatus("A");
        }

        switch (txn.getTransactionType()) {
            case "BU" -> {
                // P210-CALC-BUY: ADD TXN-QUANTITY TO POS-QUANTITY
                position.setQuantity(position.getQuantity().add(txn.getQuantity()));
                position.setCostBasis(position.getCostBasis().add(txn.getAmount()));
                position.setMarketValue(position.getMarketValue().add(txn.getAmount()));
            }
            case "SL" -> {
                // P220-CALC-SELL: SUBTRACT TXN-QUANTITY FROM POS-QUANTITY
                // Cost basis reduction is proportional to quantity sold
                BigDecimal currentQty = position.getQuantity();
                if (currentQty.signum() <= 0 || txn.getQuantity().compareTo(currentQty) > 0) {
                    log.warn("POSUPD00: Sell quantity {} exceeds position quantity {} for portfolio={}, investment={}",
                            txn.getQuantity(), currentQty, txn.getPortfolioId(), txn.getInvestmentId());
                    return false;
                }
                BigDecimal ratio = txn.getQuantity().divide(currentQty, 10, RoundingMode.HALF_UP);
                BigDecimal costReduction = position.getCostBasis().multiply(ratio)
                        .setScale(2, RoundingMode.HALF_UP);
                position.setCostBasis(position.getCostBasis().subtract(costReduction));
                BigDecimal marketValueReduction = position.getMarketValue().multiply(ratio)
                        .setScale(2, RoundingMode.HALF_UP);
                position.setQuantity(currentQty.subtract(txn.getQuantity()));
                position.setMarketValue(position.getMarketValue().subtract(marketValueReduction));
            }
            default -> {
                // Transfer or Fee - no position quantity change
                // Only update timestamp on existing positions
            }
        }

        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("BATCH");
        positionRepository.save(position);
        return true;
    }
}
