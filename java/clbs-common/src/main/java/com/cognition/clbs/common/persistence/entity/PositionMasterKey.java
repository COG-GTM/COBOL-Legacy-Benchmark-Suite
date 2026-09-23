package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** Composite primary key of {@link PositionMaster}. */
@Embeddable
public record PositionMasterKey(
    @Column(name = "PORTFOLIO_ID", nullable = false, length = 8) String portfolioId,
    @Column(name = "POSITION_DATE", nullable = false) LocalDate positionDate,
    @Column(name = "INVESTMENT_ID", nullable = false, length = 10) String investmentId) {}
