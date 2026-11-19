package com.clbs.portfolio.web.dto;

import com.clbs.portfolio.domain.Portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioResponse(String portfolioId, String accountNo, String clientName, String clientType,
                                String status, LocalDate createDate, LocalDate lastMaintDate, BigDecimal totalValue,
                                BigDecimal cashBalance, String lastUser) {

    public static PortfolioResponse from(Portfolio p) {
        return new PortfolioResponse(p.getPortfolioId(), p.getAccountNo(), p.getClientName(), p.getClientType(),
                p.getStatus(), p.getCreateDate(), p.getLastMaintDate(), p.getTotalValue(), p.getCashBalance(),
                p.getLastUser());
    }
}
