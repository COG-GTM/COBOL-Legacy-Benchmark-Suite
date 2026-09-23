package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.LocalTime;

/** Composite primary key of {@link PortfolioTransaction}. */
@Embeddable
public record PortfolioTransactionKey(
    @Column(name = "TRANSACTION_DATE", nullable = false) LocalDate transactionDate,
    @Column(name = "TRANSACTION_TIME", nullable = false) LocalTime transactionTime,
    @Column(name = "PORTFOLIO_ID", nullable = false, length = 8) String portfolioId,
    @Column(name = "SEQUENCE_NO", nullable = false, length = 6) String sequenceNumber) {}
