package com.portfolio.service;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.InvestmentPositionKey;
import com.portfolio.model.TransactionHistory;
import com.portfolio.model.enums.TransactionType;
import com.portfolio.repository.InvestmentPositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Position Update Service.
 * Replaces: POSUPD00.cbl (Position Update program).
 *
 * Implements position recalculation:
 * - BUY: add to quantity and cost basis
 * - SELL: subtract from quantity and cost basis
 * - Recalculate market value
 *
 * ALL arithmetic uses BigDecimal with RoundingMode.HALF_UP
 * to match COBOL fixed-point behavior.
 */
@Service
public class PositionUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PositionUpdateService.class);

    private final InvestmentPositionRepository positionRepository;

    public PositionUpdateService(InvestmentPositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * Updates an investment position based on a validated transaction.
     * Replaces the main processing loop in POSUPD00.cbl.
     */
    @Transactional
    public InvestmentPosition updatePosition(TransactionHistory transaction, String userId) {
        InvestmentPositionKey key = new InvestmentPositionKey(
                transaction.getPortfolioId(),
                transaction.getInvestmentId(),
                transaction.getTransactionDate()
        );

        Optional<InvestmentPosition> existingOpt = positionRepository.findById(key);
        InvestmentPosition position;

        if (existingOpt.isPresent()) {
            position = existingOpt.get();
        } else {
            position = createNewPosition(key, transaction, userId);
        }

        String transType = transaction.getTransactionType();
        switch (transType) {
            case "BU" -> processBuy(position, transaction);
            case "SL" -> processSell(position, transaction);
            case "TR" -> processTransfer(position, transaction);
            case "FE" -> processFee(position, transaction);
            default -> throw new IllegalArgumentException("Unknown transaction type: " + transType);
        }

        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser(userId);

        log.info("Updated position for portfolio {} investment {} on {}: qty={}, cost={}, value={}",
                key.getPortfolioId(), key.getInvestmentId(), key.getPositionDate(),
                position.getQuantity(), position.getCostBasis(), position.getMarketValue());

        return positionRepository.save(position);
    }

    /**
     * Creates a new position record.
     * Replaces the INITIALIZE + MOVE logic for new records in POSUPD00.cbl.
     */
    private InvestmentPosition createNewPosition(InvestmentPositionKey key,
                                                  TransactionHistory transaction,
                                                  String userId) {
        InvestmentPosition position = new InvestmentPosition();
        position.setKey(key);
        position.setQuantity(BigDecimal.ZERO);
        position.setCostBasis(BigDecimal.ZERO);
        position.setMarketValue(BigDecimal.ZERO);
        position.setCurrencyCode(transaction.getCurrencyCode());
        position.setStatus("A");
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser(userId);
        return position;
    }

    /**
     * Process BUY transaction: add to quantity and cost basis.
     * Replaces POSUPD00 BUY processing paragraph.
     */
    private void processBuy(InvestmentPosition position, TransactionHistory transaction) {
        position.setQuantity(
                position.getQuantity().add(transaction.getQuantity())
                        .setScale(4, RoundingMode.HALF_UP)
        );
        position.setCostBasis(
                position.getCostBasis().add(transaction.getAmount())
                        .setScale(2, RoundingMode.HALF_UP)
        );
        recalculateMarketValue(position, transaction.getPrice());
    }

    /**
     * Process SELL transaction: subtract from quantity and cost basis.
     * Replaces POSUPD00 SELL processing paragraph.
     */
    private void processSell(InvestmentPosition position, TransactionHistory transaction) {
        position.setQuantity(
                position.getQuantity().subtract(transaction.getQuantity())
                        .setScale(4, RoundingMode.HALF_UP)
        );

        // Calculate proportional cost basis reduction
        if (position.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costPerUnit = position.getCostBasis()
                    .divide(position.getQuantity().add(transaction.getQuantity()),
                            4, RoundingMode.HALF_UP);
            BigDecimal costReduction = costPerUnit.multiply(transaction.getQuantity())
                    .setScale(2, RoundingMode.HALF_UP);
            position.setCostBasis(
                    position.getCostBasis().subtract(costReduction)
                            .setScale(2, RoundingMode.HALF_UP)
            );
        } else {
            position.setCostBasis(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        recalculateMarketValue(position, transaction.getPrice());
    }

    /**
     * Process TRANSFER transaction.
     * Replaces POSUPD00 TRANSFER processing paragraph.
     */
    private void processTransfer(InvestmentPosition position, TransactionHistory transaction) {
        position.setQuantity(
                position.getQuantity().add(transaction.getQuantity())
                        .setScale(4, RoundingMode.HALF_UP)
        );
        position.setCostBasis(
                position.getCostBasis().add(transaction.getAmount())
                        .setScale(2, RoundingMode.HALF_UP)
        );
        recalculateMarketValue(position, transaction.getPrice());
    }

    /**
     * Process FEE transaction: reduce cost basis by fee amount.
     * Replaces POSUPD00 FEE processing paragraph.
     */
    private void processFee(InvestmentPosition position, TransactionHistory transaction) {
        position.setCostBasis(
                position.getCostBasis().subtract(transaction.getAmount())
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }

    /**
     * Recalculate market value = quantity * current price.
     * Uses BigDecimal with HALF_UP to match COBOL rounding.
     */
    private void recalculateMarketValue(InvestmentPosition position, BigDecimal currentPrice) {
        position.setMarketValue(
                position.getQuantity().multiply(currentPrice)
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }
}
