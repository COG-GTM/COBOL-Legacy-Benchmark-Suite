package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.LocalTime;

/** Composite primary key of {@link PositionHistory}. */
@Embeddable
public record PositionHistoryKey(
    @Column(name = "ACCOUNT_NO", nullable = false, length = 8) String accountNumber,
    @Column(name = "PORTFOLIO_ID", nullable = false, length = 10) String portfolioId,
    @Column(name = "TRANS_DATE", nullable = false) LocalDate transactionDate,
    @Column(name = "TRANS_TIME", nullable = false) LocalTime transactionTime) {}
