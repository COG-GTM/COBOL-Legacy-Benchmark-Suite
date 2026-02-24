package com.portfolio.batch;

import com.portfolio.entity.InvestmentPosition;
import com.portfolio.entity.InvestmentPositionId;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
            updatePosition(txn);
            updateCount++;
        }

        log.info("POSUPD00: Completed. Positions updated={}", updateCount);

        return RepeatStatus.FINISHED;
    }

    /**
     * Update position based on transaction - replaces P200-UPDATE-POSITION.
     */
    private void updatePosition(TransactionHistory txn) {
        InvestmentPositionId positionId = new InvestmentPositionId();
        positionId.setPortfolioId(txn.getPortfolioId());
        positionId.setInvestmentId(txn.getInvestmentId());
        positionId.setPositionDate(LocalDate.now());

        Optional<InvestmentPosition> existingOpt = positionRepository.findById(positionId);

        InvestmentPosition position;
        if (existingOpt.isPresent()) {
            position = existingOpt.get();
        } else {
            position = new InvestmentPosition();
            position.setPortfolioId(txn.getPortfolioId());
            position.setInvestmentId(txn.getInvestmentId());
            position.setPositionDate(LocalDate.now());
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
                position.setQuantity(position.getQuantity().subtract(txn.getQuantity()));
                position.setCostBasis(position.getCostBasis().subtract(txn.getAmount()));
                position.setMarketValue(position.getMarketValue().subtract(txn.getAmount()));
            }
            default -> {
                // Transfer or Fee - no position quantity change
            }
        }

        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("BATCH");
        positionRepository.save(position);
    }
}
