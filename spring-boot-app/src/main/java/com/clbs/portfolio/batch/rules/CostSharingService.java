package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostSharingService {

    private final PositionRepository positionRepository;

    public AdjudicationResult apply(TransactionRecord transaction) {
        if (transaction.getTrnType() != TransactionType.SL) {
            transaction.setCostBasisAdjustment(BigDecimal.ZERO);
            return AdjudicationResult.APPROVED;
        }

        Optional<Position> positionOpt = positionRepository
                .findByPortfolioIdAndInvestmentId(transaction.getPortfolioId(), transaction.getInvestmentId());

        if (positionOpt.isEmpty()) {
            log.warn("No position found for SELL: portfolio={}, investment={}",
                    transaction.getPortfolioId(), transaction.getInvestmentId());
            return AdjudicationResult.DENIED;
        }

        Position position = positionOpt.get();
        BigDecimal currentQuantity = position.getQuantity();

        if (currentQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Position quantity is zero or negative for portfolio={}", transaction.getPortfolioId());
            return AdjudicationResult.DENIED;
        }

        BigDecimal sellQuantity = transaction.getQuantity().abs();

        if (sellQuantity.compareTo(currentQuantity) > 0) {
            log.warn("Sell quantity {} exceeds position quantity {} for portfolio={}",
                    sellQuantity, currentQuantity, transaction.getPortfolioId());
            return AdjudicationResult.DENIED;
        }

        BigDecimal weightedAvgCost = position.getCostBasis()
                .divide(currentQuantity, 6, RoundingMode.HALF_UP);
        BigDecimal costBasisAdjustment = weightedAvgCost.multiply(sellQuantity)
                .setScale(2, RoundingMode.HALF_UP);

        transaction.setCostBasisAdjustment(costBasisAdjustment);

        log.debug("Cost basis adjustment for SELL: {}", costBasisAdjustment);
        return AdjudicationResult.APPROVED;
    }
}
