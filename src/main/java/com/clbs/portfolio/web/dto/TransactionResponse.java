package com.clbs.portfolio.web.dto;

import com.clbs.portfolio.domain.PortfolioTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TransactionResponse(Long id, String portfolioId, String investmentId, String type,
                                  LocalDate transactionDate, LocalTime transactionTime, BigDecimal quantity,
                                  BigDecimal price, BigDecimal amount, String currency, String status,
                                  String statusMessage, LocalDateTime processTimestamp, String processUser) {

    public static TransactionResponse from(PortfolioTransaction t) {
        return new TransactionResponse(t.getId(), t.getPortfolioId(), t.getInvestmentId(), t.getType(),
                t.getTransactionDate(), t.getTransactionTime(), t.getQuantity(), t.getPrice(), t.getAmount(),
                t.getCurrency(), t.getStatus(), t.getStatusMessage(), t.getProcessTimestamp(), t.getProcessUser());
    }
}
