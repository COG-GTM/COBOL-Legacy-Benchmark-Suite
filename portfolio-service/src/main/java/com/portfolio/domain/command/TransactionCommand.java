package com.portfolio.domain.command;

import com.portfolio.domain.model.TransactionType;
import java.math.BigDecimal;

/**
 * Immutable command DTO — maps COBOL TRNREC.cpy TRANSACTION-RECORD.
 */
public record TransactionCommand(
        String transactionDate,
        String transactionTime,
        String portfolioId,
        String sequenceNumber,
        String investmentId,
        TransactionType type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal amount,
        String currency,
        String processUser
) {}
