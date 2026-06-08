package com.portfolio.domain.event;

import com.portfolio.domain.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event emitted after a transaction is applied to a portfolio.
 * Consumed by the audit subsystem.
 */
public record TransactionProcessedEvent(
        String portfolioId,
        String accountNumber,
        TransactionType transactionType,
        BigDecimal amount,
        String userId,
        LocalDateTime timestamp
) {}
