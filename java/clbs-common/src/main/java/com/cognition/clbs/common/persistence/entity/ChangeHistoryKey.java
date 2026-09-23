package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.LocalTime;

/** Composite primary key of {@link ChangeHistory}. */
@Embeddable
public record ChangeHistoryKey(
    @Column(name = "PORTFOLIO_ID", nullable = false, length = 8) String portfolioId,
    @Column(name = "HISTORY_DATE", nullable = false) LocalDate historyDate,
    @Column(name = "HISTORY_TIME", nullable = false) LocalTime historyTime,
    @Column(name = "SEQUENCE_NO", nullable = false, length = 4) String sequenceNumber) {}
