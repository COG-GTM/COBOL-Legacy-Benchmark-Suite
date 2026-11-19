package com.clbs.portfolio.web.dto;

import com.clbs.portfolio.domain.Position;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PositionResponse(String portfolioId, String investmentId, LocalDate positionDate, BigDecimal quantity,
                               BigDecimal costBasis, BigDecimal marketValue, String currency, String status) {

    public static PositionResponse from(Position p) {
        return new PositionResponse(p.getPortfolioId(), p.getInvestmentId(), p.getPositionDate(), p.getQuantity(),
                p.getCostBasis(), p.getMarketValue(), p.getCurrency(), p.getStatus());
    }
}
