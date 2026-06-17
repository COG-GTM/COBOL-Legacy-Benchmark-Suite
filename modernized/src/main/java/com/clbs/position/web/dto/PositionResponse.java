package com.clbs.position.web.dto;

import com.clbs.position.domain.PositionCalculator;
import com.clbs.position.entity.Position;

import java.math.BigDecimal;

/**
 * REST representation of a position, enriched with the two derived analytics the
 * COBOL system computed on demand: weighted-average unit cost and unrealized
 * (mark-to-market) gain/loss.
 */
public record PositionResponse(
        Long id,
        String portfolioId,
        String positionDate,
        String investmentId,
        BigDecimal quantity,
        BigDecimal costBasis,
        BigDecimal marketValue,
        BigDecimal averageCost,
        BigDecimal unrealizedGainLoss,
        String currency,
        String status) {

    public static PositionResponse from(Position p) {
        return new PositionResponse(
                p.getId(),
                p.getPortfolioId(),
                p.getPositionDate(),
                p.getInvestmentId(),
                p.getQuantity(),
                p.getCostBasis(),
                p.getMarketValue(),
                PositionCalculator.averageCost(p.toState()),
                PositionCalculator.unrealizedGainLoss(p.toState()),
                p.getCurrency(),
                p.getStatus());
    }
}
