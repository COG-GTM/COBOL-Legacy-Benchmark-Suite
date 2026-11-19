package com.clbs.portfolio.web.dto;

import com.clbs.portfolio.domain.PositionHistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record HistoryResponse(String accountNo, String portfolioId, LocalDate transDate, LocalTime transTime,
                              String transType, String securityId, BigDecimal quantity, BigDecimal price,
                              BigDecimal amount, BigDecimal costBasis) {

    public static HistoryResponse from(PositionHistory h) {
        return new HistoryResponse(h.getAccountNo(), h.getPortfolioId(), h.getTransDate(), h.getTransTime(),
                h.getTransType(), h.getSecurityId(), h.getQuantity(), h.getPrice(), h.getAmount(), h.getCostBasis());
    }
}
